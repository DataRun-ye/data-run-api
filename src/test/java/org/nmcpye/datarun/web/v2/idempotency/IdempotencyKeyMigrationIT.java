package org.nmcpye.datarun.web.v2.idempotency;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.changelog.FastCheckService;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The idempotency changeset on its own, against PostgreSQL: it adds the api schema and its one table with no
 * column that could hold a value, runs once, rolls back, refuses to drop a schema someone else used, and its
 * constraints keep a row either claimed or completed.
 */
class IdempotencyKeyMigrationIT {

    private static final String CHANGELOG = "config/liquibase/changelog/api/20261125-idempotency-key.xml";
    private static final String CHANGESET = "20261125-e0-01-idempotency-key";

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");

    private static JdbcTemplate db;

    @BeforeAll
    static void startDatabase() {
        PG.start();
        db = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    @BeforeEach
    void startFromNothing() {
        db.execute("DROP SCHEMA IF EXISTS api CASCADE");
        db.execute("DROP TABLE IF EXISTS databasechangelog");
        db.execute("DROP TABLE IF EXISTS databasechangeloglock");
    }

    /** One Liquibase run, as a fresh process would make it. */
    private static void liquibase(boolean rollback) throws Exception {
        ChangeLogHistoryServiceFactory.getInstance().resetAll();
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);
            if (rollback) {
                liquibase.rollback(1, (String) null);
            } else {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    private static boolean schemaExists() {
        return Boolean.TRUE.equals(db.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'api')", Boolean.class));
    }

    private static List<String> runs() {
        return db.queryForList("SELECT exectype FROM databasechangelog WHERE id = ?", String.class, CHANGESET);
    }

    @Test
    void itAddsTheTableWithIdsAHashAndVersionsOnlyOnceAndRollsBack() throws Exception {
        liquibase(false);

        List<Map<String, Object>> columns = db.queryForList("""
            SELECT column_name, data_type, character_maximum_length, is_nullable
            FROM information_schema.columns
            WHERE table_schema = 'api' AND table_name = 'idempotency_key'
            ORDER BY ordinal_position""");
        assertThat(columns).extracting(column -> column.get("column_name") + " " + column.get("data_type") + " "
                + column.get("character_maximum_length") + " " + column.get("is_nullable"))
            .containsExactly(
                "actor_user_id character varying 26 NO",
                "idempotency_key uuid null NO",
                "request_hash bytea null NO",
                "resource_type character varying 32 YES",
                "resource_id character varying 36 YES",
                "resource_version bigint null YES",
                "response_status smallint null YES",
                "created_at timestamp with time zone null NO",
                "expires_at timestamp with time zone null NO");
        assertThat(db.queryForObject("SELECT pg_get_indexdef('api.idx_idempotency_key_expires_at'::regclass)",
            String.class)).contains("USING btree (expires_at)");
        assertThat(db.queryForObject("""
            SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'pk_idempotency_key'""",
            String.class)).isEqualTo("PRIMARY KEY (actor_user_id, idempotency_key)");

        liquibase(false);
        assertThat(runs()).as("a second update runs nothing").containsExactly("EXECUTED");

        liquibase(true);
        assertThat(schemaExists()).isFalse();
        assertThat(runs()).isEmpty();

        liquibase(false);
        assertThat(schemaExists()).isTrue();
    }

    /** DROP SCHEMA without CASCADE: a rollback never takes anything else that was put in the schema. */
    @Test
    void theRollbackRefusesWhenTheSchemaHoldsSomethingElse() throws Exception {
        liquibase(false);
        db.execute("CREATE TABLE api.someone_elses (id int)");

        assertThatThrownBy(() -> liquibase(true)).hasStackTraceContaining("depend");

        assertThat(db.queryForObject("SELECT to_regclass('api.idempotency_key') IS NOT NULL", Boolean.class))
            .as("the refused rollback undid its own drop").isTrue();
        assertThat(db.queryForObject("SELECT to_regclass('api.someone_elses') IS NOT NULL", Boolean.class)).isTrue();
        assertThat(runs()).containsExactly("EXECUTED");
    }

    @Test
    void aRowIsEitherClaimedOrCompletedWithASuccessStatus() throws Exception {
        liquibase(false);
        String insert = """
            INSERT INTO api.idempotency_key (actor_user_id, idempotency_key, request_hash, resource_type, resource_id,
                                             resource_version, response_status, expires_at)
            VALUES ('u1', gen_random_uuid(), %s, %s, %s, %s, %s, now() + %s)""";
        String hash = "decode(repeat('ab', 32), 'hex')";

        db.update(insert.formatted(hash, "NULL", "NULL", "NULL", "NULL", "interval '1 hour'"));
        db.update(insert.formatted(hash, "'thing'", "'t1'", "3", "200", "interval '1 hour'"));

        assertThatThrownBy(() -> db.update(insert.formatted("decode('ab', 'hex')", "NULL", "NULL", "NULL", "NULL",
            "interval '1 hour'"))).as("a hash is 32 bytes").hasStackTraceContaining("ck_idempotency_key_hash");
        assertThatThrownBy(() -> db.update(insert.formatted(hash, "'thing'", "NULL", "3", "200",
            "interval '1 hour'"))).as("all of the outcome or none").hasStackTraceContaining("ck_idempotency_key_outcome");
        assertThatThrownBy(() -> db.update(insert.formatted(hash, "'thing'", "'t1'", "3", "412",
            "interval '1 hour'"))).as("only successes are kept").hasStackTraceContaining("ck_idempotency_key_status");
        assertThatThrownBy(() -> db.update(insert.formatted(hash, "NULL", "NULL", "NULL", "NULL",
            "interval '-1 hour'"))).hasStackTraceContaining("ck_idempotency_key_expiry");
    }
}
