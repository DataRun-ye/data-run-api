package org.nmcpye.datarun.etl;

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
import org.junit.jupiter.api.Timeout;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The changeset that drops analytics.etl_runs, on its own against PostgreSQL: it drops that old copy and leaves the
 * ETL's run log public.etl_runs alone, runs once, rolls back to the same definition, skips a table another role
 * owns instead of failing the deploy, and fails after its lock timeout instead of waiting behind a reader.
 */
class AnalyticsEtlRunsDropMigrationIT {

    private static final String CHANGELOG = "config/liquibase/changelog/etl/20261125-drop-analytics-etl-runs.xml";
    private static final String CHANGESET = "20261125-05-drop-analytics-etl-runs";

    /** The old copy as the staging clone of production has it. */
    private static final String OLD_COPY = """
        CREATE TABLE analytics.etl_runs (
          run_id text NOT NULL, started_at timestamp with time zone, finished_at timestamp with time zone,
          status text, row_counts jsonb, error_message text, CONSTRAINT etl_runs_pkey PRIMARY KEY (run_id))""";

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");

    private static JdbcTemplate db;

    @BeforeAll
    static void startDatabase() {
        PG.start();
        db = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        db.execute("CREATE SCHEMA analytics");
        db.execute("CREATE TABLE public.etl_runs (etl_run_id bigserial PRIMARY KEY, success_count integer)");
        db.update("INSERT INTO public.etl_runs (success_count) SELECT i FROM generate_series(1, 10) AS i");
        db.execute("CREATE ROLE someone_else");
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    @BeforeEach
    void startFromTheOldCopyAndNoHistory() {
        db.execute("DROP TABLE IF EXISTS databasechangelog");
        db.execute("DROP TABLE IF EXISTS databasechangeloglock");
        db.execute("DROP TABLE IF EXISTS analytics.etl_runs");
        db.execute(OLD_COPY);
        db.update("INSERT INTO analytics.etl_runs (run_id, status) SELECT 'run' || i, 'done' FROM generate_series(1, 5) AS i");
    }

    @Test
    void itDropsTheOldCopyOnceKeepsTheRunLogAndRollsBackToTheDefinition() throws Exception {
        List<String> definition = definition();

        liquibase(false);
        assertThat(exists()).isFalse();
        assertThat(db.queryForObject("SELECT count(*) FROM public.etl_runs", Integer.class)).isEqualTo(10);

        liquibase(false);
        assertThat(runs()).as("a second update runs nothing").containsExactly("EXECUTED");

        liquibase(true);
        assertThat(exists()).isTrue();
        assertThat(definition()).as("the rollback restores the definition").isEqualTo(definition);
        assertThat(runs()).isEmpty();
    }

    @Test
    void aTableAnotherRoleOwnsIsSkippedNotFailed() throws Exception {
        db.execute("ALTER TABLE analytics.etl_runs OWNER TO someone_else");

        liquibase(false);

        assertThat(exists()).isTrue();
        assertThat(runs()).as("skipped, to be tried again at the next start").isEmpty();
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void aReaderMakesTheDropFailAfterItsLockTimeoutInsteadOfWaiting() throws Exception {
        try (Connection reader = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            reader.setAutoCommit(false);
            try (Statement statement = reader.createStatement()) {
                statement.execute("LOCK TABLE analytics.etl_runs IN ACCESS SHARE MODE");
            }
            long start = System.nanoTime();
            assertThatThrownBy(() -> liquibase(false, "2s")).hasStackTraceContaining("lock timeout");
            assertThat((System.nanoTime() - start) / 1e9).isBetween(1.5, 20.0);
            reader.rollback();
        }

        assertThat(exists()).isTrue();
        assertThat(runs()).isEmpty();
    }

    private static void liquibase(boolean rollback) throws Exception {
        liquibase(rollback, null);
    }

    /** One Liquibase run, as a fresh process would make it (see OutboxUnfinishedIndexMigrationIT). */
    private static void liquibase(boolean rollback, String lockTimeout) throws Exception {
        ChangeLogHistoryServiceFactory.getInstance().resetAll();
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);
            if (lockTimeout != null) {
                liquibase.setChangeLogParameter("analyticsEtlRunsLockTimeout", lockTimeout);
            }
            if (rollback) {
                liquibase.rollback(1, (String) null);
            } else {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    private static boolean exists() {
        return Boolean.TRUE.equals(db.queryForObject(
            "SELECT to_regclass('analytics.etl_runs') IS NOT NULL", Boolean.class));
    }

    /** The columns with their types and nullability, and the constraints. */
    private static List<String> definition() {
        List<String> columns = db.queryForList("""
            SELECT a.attname || ' ' || format_type(a.atttypid, a.atttypmod) || CASE WHEN a.attnotnull THEN ' NOT NULL' ELSE '' END
            FROM pg_attribute a WHERE a.attrelid = 'analytics.etl_runs'::regclass AND a.attnum > 0 AND NOT a.attisdropped
            ORDER BY a.attnum""", String.class);
        List<String> constraints = db.queryForList("""
            SELECT conname || ' ' || pg_get_constraintdef(oid) FROM pg_constraint
            WHERE conrelid = 'analytics.etl_runs'::regclass ORDER BY conname""", String.class);
        return java.util.stream.Stream.concat(columns.stream(), constraints.stream()).toList();
    }

    private static List<String> runs() {
        return db.queryForList("SELECT exectype FROM databasechangelog WHERE id = ?", String.class, CHANGESET);
    }
}
