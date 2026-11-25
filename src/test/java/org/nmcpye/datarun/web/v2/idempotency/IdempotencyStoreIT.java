package org.nmcpye.datarun.web.v2.idempotency;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The idempotency record's SQL with two real sessions: a duplicate claim waits on the key and then sees the
 * committed outcome, or claims when the first rolled back, or gives up at the lock timeout; keys are per caller;
 * an expired key is claimed again in place; the purge deletes expired keys only, in batches, skipping held ones.
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class IdempotencyStoreIT {

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");
    private static final Duration RETENTION = Duration.ofHours(72);
    private static final byte[] HASH = hash(1);
    private static final CommandOutcome OUTCOME = CommandOutcome.ok("thing", "01JDQ3ZK5V8Y7M2N4P6R8T0W2X", 4);

    private static JdbcTemplate db;

    private final ExecutorService threads = Executors.newCachedThreadPool();
    private final List<Session> sessions = new ArrayList<>();

    @BeforeAll
    static void startDatabase() throws Exception {
        PG.start();
        db = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase("config/liquibase/changelog/api/20261125-idempotency-key.xml",
                new ClassLoaderResourceAccessor(), database).update(new Contexts(), new LabelExpression());
        }
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    @BeforeEach
    void empty() {
        db.update("DELETE FROM api.idempotency_key");
    }

    @AfterEach
    void close() throws Exception {
        for (Session session : sessions) {
            session.close();
        }
        threads.shutdownNow();
    }

    /** A session with its own connection and transaction, as one request's command transaction would be. */
    private final class Session implements AutoCloseable {
        final Connection connection;
        final IdempotencyStore store;

        Session() throws SQLException {
            connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
            connection.setAutoCommit(false);
            store = new IdempotencyStore(new NamedParameterJdbcTemplate(new SingleConnectionDataSource(connection, true)));
            sessions.add(this);
        }

        void commit() throws SQLException {
            connection.commit();
        }

        void rollback() throws SQLException {
            connection.rollback();
        }

        @Override
        public void close() throws SQLException {
            if (!connection.isClosed()) {
                connection.rollback();
                connection.close();
            }
        }
    }

    private static byte[] hash(int seed) {
        byte[] hash = new byte[32];
        java.util.Arrays.fill(hash, (byte) seed);
        return hash;
    }

    @Test
    void aCommittedKeyIsFoundWithItsFingerprintAndOutcomeAndKeysArePerCaller() throws Exception {
        UUID key = UUID.randomUUID();
        Session first = new Session();
        assertThat(first.store.claim("u1", key, HASH, RETENTION)).isInstanceOf(IdempotencyStore.Claimed.class);
        first.store.recordOutcome("u1", key, OUTCOME);
        first.commit();

        Session second = new Session();
        IdempotencyStore.Claim found = second.store.claim("u1", key, hash(2), RETENTION);
        assertThat(found).isInstanceOfSatisfying(IdempotencyStore.Completed.class, completed -> {
            assertThat(completed.requestHash()).isEqualTo(HASH);
            assertThat(completed.outcome()).isEqualTo(OUTCOME);
        });
        assertThat(second.store.claim("u2", key, hash(2), RETENTION))
            .as("another caller's same key").isInstanceOf(IdempotencyStore.Claimed.class);
        second.rollback();
        assertThat(db.queryForObject("SELECT request_hash FROM api.idempotency_key WHERE actor_user_id = 'u1'",
            byte[].class)).as("a lookup changes nothing").isEqualTo(HASH);
    }

    @Test
    void aDuplicateWaitsOnTheKeyThenSeesTheCommittedOutcome() throws Exception {
        UUID key = UUID.randomUUID();
        Session first = new Session();
        first.store.claim("u1", key, HASH, RETENTION);
        Session second = new Session();
        CompletableFuture<IdempotencyStore.Claim> waiting =
            CompletableFuture.supplyAsync(() -> second.store.claim("u1", key, HASH, RETENTION), threads);

        Thread.sleep(500);
        assertThat(waiting).as("the duplicate waits on the key").isNotDone();
        first.store.recordOutcome("u1", key, OUTCOME);
        first.commit();

        assertThat(waiting.get(10, TimeUnit.SECONDS)).isInstanceOfSatisfying(IdempotencyStore.Completed.class,
            completed -> assertThat(completed.outcome()).isEqualTo(OUTCOME));
    }

    @Test
    void aDuplicateClaimsTheKeyWhenTheFirstAttemptRollsBack() throws Exception {
        UUID key = UUID.randomUUID();
        Session first = new Session();
        first.store.claim("u1", key, HASH, RETENTION);
        Session second = new Session();
        CompletableFuture<IdempotencyStore.Claim> waiting =
            CompletableFuture.supplyAsync(() -> second.store.claim("u1", key, HASH, RETENTION), threads);

        Thread.sleep(500);
        assertThat(waiting).isNotDone();
        first.rollback();

        assertThat(waiting.get(10, TimeUnit.SECONDS)).isInstanceOf(IdempotencyStore.Claimed.class);
    }

    @Test
    void aDuplicateGivesUpAtTheLockTimeoutWith409() throws Exception {
        UUID key = UUID.randomUUID();
        Session first = new Session();
        first.store.claim("u1", key, HASH, RETENTION);
        Session second = new Session();
        second.store.limitLockWaits(Duration.ofMillis(400));

        long start = System.nanoTime();
        assertThatThrownBy(() -> second.store.claim("u1", key, HASH, RETENTION))
            .isInstanceOfSatisfying(ApiProblemException.class, problem -> {
                assertThat(problem.code()).isEqualTo(ProblemCode.REQUEST_IN_PROGRESS);
                assertThat(problem.headers().getFirst("Retry-After")).isEqualTo("1");
            });
        assertThat((System.nanoTime() - start) / 1e9).isBetween(0.3, 5.0);
        first.rollback();
    }

    @Test
    void anExpiredKeyIsClaimedAgainInPlaceAndARolledBackClaimLeavesItAsItWas() throws Exception {
        UUID key = UUID.randomUUID();
        db.update("""
            INSERT INTO api.idempotency_key (actor_user_id, idempotency_key, request_hash, resource_type, resource_id,
                                             resource_version, response_status, created_at, expires_at)
            VALUES ('u1', ?, ?, 'thing', 't1', 3, 200, now() - interval '80 hours', now() - interval '8 hours')""",
            key, HASH);

        Session session = new Session();
        assertThat(session.store.claim("u1", key, hash(2), RETENTION)).isInstanceOf(IdempotencyStore.Claimed.class);
        session.rollback();
        assertThat(db.queryForObject("SELECT resource_version FROM api.idempotency_key", Long.class)).isEqualTo(3L);

        assertThat(session.store.claim("u1", key, hash(2), RETENTION)).isInstanceOf(IdempotencyStore.Claimed.class);
        session.store.recordOutcome("u1", key, OUTCOME);
        session.commit();
        assertThat(db.queryForObject("SELECT expires_at > now() + interval '71 hours' FROM api.idempotency_key",
            Boolean.class)).isTrue();
        assertThat(db.queryForObject("SELECT request_hash FROM api.idempotency_key", byte[].class))
            .isEqualTo(hash(2));
    }

    @Test
    void anOutcomeIsRecordedOnlyOnAKeyThisTransactionClaimed() throws Exception {
        Session session = new Session();
        assertThatThrownBy(() -> session.store.recordOutcome("u1", UUID.randomUUID(), OUTCOME))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void thePurgeDeletesExpiredKeysInBatchesAndSkipsAHeldOne() throws Exception {
        db.update("""
            INSERT INTO api.idempotency_key (actor_user_id, idempotency_key, request_hash, resource_type, resource_id,
                                             resource_version, response_status, created_at, expires_at)
            SELECT 'u' || i, gen_random_uuid(), decode(repeat('ab', 32), 'hex'), 'thing', 't' || i, 1, 200,
                   now() - interval '100 hours', now() + CASE WHEN i <= 5 THEN interval '-1 hour' ELSE interval '1 hour' END
            FROM generate_series(1, 7) AS i""");
        UUID held = db.queryForObject("SELECT idempotency_key FROM api.idempotency_key WHERE actor_user_id = 'u1'",
            UUID.class);
        Session claiming = new Session();
        claiming.store.claim("u1", held, HASH, RETENTION);

        Session purging = new Session();
        assertThat(purging.store.purgeExpired(2)).isEqualTo(2);
        assertThat(purging.store.purgeExpired(2)).isEqualTo(2);
        assertThat(purging.store.purgeExpired(2)).as("the held key is skipped").isZero();
        purging.commit();
        claiming.rollback();

        assertThat(db.queryForList("SELECT actor_user_id FROM api.idempotency_key ORDER BY 1", String.class))
            .containsExactly("u1", "u6", "u7");
    }
}
