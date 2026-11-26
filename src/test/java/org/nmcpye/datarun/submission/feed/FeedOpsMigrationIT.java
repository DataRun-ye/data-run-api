package org.nmcpye.datarun.submission.feed;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The publisher's bookkeeping changeset after the feed's schema, against PostgreSQL 16: three tables in feed_ops
 * (pending changes, republish marks, normalization flags) with their keys, the index the retries claim through, a
 * rollback that leaves the feed's schema as it was, and the feed schema's own rollback after it.
 */
class FeedOpsMigrationIT {

    static final String CHANGELOG = "config/liquibase/changelog/feed/20261125-feed-ops.xml";
    static final String CHANGESET = "20261125-d03-feed-ops";

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
        db.execute("DROP SCHEMA IF EXISTS feed CASCADE");
        db.execute("DROP SCHEMA IF EXISTS feed_ops CASCADE");
        db.execute("DROP TABLE IF EXISTS databasechangelog");
        db.execute("DROP TABLE IF EXISTS databasechangeloglock");
    }

    /** One Liquibase run of one changelog, as a fresh process would make it. */
    private static void liquibase(String changelog, boolean rollback) throws Exception {
        ChangeLogHistoryServiceFactory.getInstance().resetAll();
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
            if (rollback) {
                liquibase.rollback(1, (String) null);
            } else {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    private static void migrate() throws Exception {
        liquibase(FeedSchemaMigrationIT.CHANGELOG, false);
        liquibase(CHANGELOG, false);
    }

    private static List<String> columns(String table) {
        return db.queryForList("""
            SELECT column_name || ' ' || data_type || ' ' || is_nullable
            FROM information_schema.columns
            WHERE table_schema = 'feed_ops' AND table_name = ?
            ORDER BY column_name""", String.class, table);
    }

    private static String constraint(String name) {
        return db.queryForObject("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = ?",
            String.class, name);
    }

    @Test
    void theBookkeepingTablesHoldIdsCodesAndTimesOnly() throws Exception {
        migrate();

        assertThat(db.queryForList("""
                SELECT table_name FROM information_schema.tables WHERE table_schema = 'feed_ops' ORDER BY table_name""",
            String.class))
            .containsExactly("normalization_flags", "pending", "republish");
        assertThat(columns("pending")).containsExactly(
            "attempts integer NO",
            "dead_at timestamp with time zone YES",
            "first_failed_at timestamp with time zone NO",
            "last_failed_at timestamp with time zone NO",
            "next_attempt_at timestamp with time zone NO",
            "reason text NO",
            "submission_id text NO");
        assertThat(columns("republish")).containsExactly(
            "attempts integer NO",
            "last_failed_at timestamp with time zone YES",
            "marked_at timestamp with time zone NO",
            "next_attempt_at timestamp with time zone YES",
            "reason text YES",
            "submission_id text NO");
        assertThat(columns("normalization_flags")).containsExactly(
            "flags jsonb NO",
            "submission_id text NO",
            "version bigint NO");
    }

    @Test
    void eachTableIsKeyedBySubmissionAndTheRetriesClaimThroughAnIndex() throws Exception {
        migrate();

        assertThat(constraint("pk_feed_ops_pending")).isEqualTo("PRIMARY KEY (submission_id)");
        assertThat(constraint("pk_feed_ops_republish")).isEqualTo("PRIMARY KEY (submission_id)");
        assertThat(constraint("pk_feed_ops_normalization_flags")).isEqualTo("PRIMARY KEY (submission_id)");
        assertThat(db.queryForObject("""
                SELECT pg_get_indexdef('feed_ops.idx_feed_ops_pending_due'::regclass)""", String.class))
            .isEqualTo("CREATE INDEX idx_feed_ops_pending_due ON feed_ops.pending USING btree (next_attempt_at) "
                + "WHERE (dead_at IS NULL)");
        assertThatThrownBy(() -> db.update("""
                INSERT INTO feed_ops.pending (submission_id, attempts, reason, first_failed_at, last_failed_at,
                                              next_attempt_at)
                VALUES ('s', 0, 'TIMEOUT', now(), now(), now())"""))
            .as("a pending change has failed at least once")
            .hasStackTraceContaining("ck_feed_ops_pending_attempts");
        assertThat(db.queryForObject("""
                SELECT count(*) FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace
                WHERE n.nspname = 'feed_ops' AND c.contype = 'f'""", Long.class))
            .as("no foreign key leaves the schema").isZero();
    }

    @Test
    void itRunsOnceAndRollsBackLeavingTheFeedSchema() throws Exception {
        migrate();
        liquibase(CHANGELOG, false);
        assertThat(db.queryForList("SELECT exectype FROM databasechangelog WHERE id = ?", String.class, CHANGESET))
            .as("a second update runs nothing").containsExactly("EXECUTED");

        liquibase(CHANGELOG, true);
        assertThat(db.queryForObject("""
                SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'feed_ops'""", Long.class))
            .as("feed_ops is empty again").isZero();
        assertThat(db.queryForObject("SELECT to_regclass('feed.submission') IS NOT NULL", Boolean.class))
            .as("the feed's schema stays").isTrue();
        assertThat(db.queryForList("SELECT id FROM databasechangelog", String.class))
            .containsExactly(FeedSchemaMigrationIT.CHANGESET);

        liquibase(CHANGELOG, false);
        liquibase(CHANGELOG, true);
        liquibase(FeedSchemaMigrationIT.CHANGELOG, true);
        assertThat(db.queryForObject("""
                SELECT count(*) FROM pg_namespace WHERE nspname IN ('feed', 'feed_ops')""", Long.class))
            .as("then the feed schema's own rollback drops both schemas").isZero();
    }

    /** The first publication's mark (changeset d04): one row at most, and a rollback that drops only it. */
    @Test
    void theFirstPublicationsMarkHoldsOneRowAndRollsBack() throws Exception {
        String firstPublication = "config/liquibase/changelog/feed/20261126-feed-first-publication.xml";
        migrate();
        liquibase(firstPublication, false);

        assertThat(columns("first_publication")).containsExactly(
            "failed bigint NO", "finished_at timestamp with time zone YES", "id boolean NO", "mark text NO",
            "requested_at timestamp with time zone NO", "skipped bigint NO", "written bigint NO");
        db.update("INSERT INTO feed_ops.first_publication (requested_at, mark) VALUES (now(), '')");
        assertThatThrownBy(() -> db.update(
            "INSERT INTO feed_ops.first_publication (id, requested_at, mark) VALUES (false, now(), '')"))
            .as("one row at most").hasStackTraceContaining("ck_feed_ops_first_publication_one");

        liquibase(firstPublication, true);
        assertThat(db.queryForObject("SELECT to_regclass('feed_ops.first_publication') IS NULL", Boolean.class))
            .isTrue();
        assertThat(db.queryForObject("SELECT to_regclass('feed_ops.pending') IS NOT NULL", Boolean.class))
            .as("the rest of feed_ops stays").isTrue();
    }
}
