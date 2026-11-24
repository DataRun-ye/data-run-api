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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The outbox index changeset on its own, against PostgreSQL: it builds a valid index, does nothing when run
 * again, rolls back, replaces an invalid index a failed concurrent build would leave, and only marks itself ran
 * when a valid index is already there.
 */
class OutboxUnfinishedIndexMigrationIT {

    private static final String CHANGELOG = "config/liquibase/changelog/etl/20261124-outbox-unfinished-index.xml";
    private static final String CHANGESET = "20261124-01-outbox-unfinished-index";

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");

    private static JdbcTemplate db;

    @BeforeAll
    static void startDatabase() {
        PG.start();
        db = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        db.execute("""
            CREATE TABLE outbox (
              outbox_id bigserial PRIMARY KEY,
              submission_serial_number bigint NOT NULL,
              submission_id varchar(255) NOT NULL,
              submission_uid varchar(11) NOT NULL,
              topic varchar(255) NOT NULL,
              status varchar(255),
              next_attempt_at timestamp,
              created_at timestamp)""");
        db.update("""
            INSERT INTO outbox (submission_serial_number, submission_id, submission_uid, topic, status, created_at)
            SELECT i, 's' || i, 'u' || i, 'version0001', CASE WHEN i % 10 = 0 THEN 'pending' ELSE 'success' END, now()
            FROM generate_series(1, 1000) AS i""");
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    @BeforeEach
    void startFromNoIndexAndNoHistory() {
        db.execute("DROP INDEX IF EXISTS idx_outbox_unfinished");
        db.execute("DROP TABLE IF EXISTS databasechangelog");
        db.execute("DROP TABLE IF EXISTS databasechangeloglock");
    }

    /**
     * One Liquibase run, as a fresh process would make it: within a process Liquibase keeps the history it read
     * and remembers a changelog it found up to date, even across a rollback.
     */
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

    /** "valid", "invalid", or null when there is no such index. */
    private static String index() {
        List<Boolean> valid = db.queryForList("""
            SELECT i.indisvalid FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid
            WHERE c.relname = 'idx_outbox_unfinished'""", Boolean.class);
        return valid.isEmpty() ? null : valid.get(0) ? "valid" : "invalid";
    }

    private static List<String> runs() {
        return db.queryForList("SELECT exectype FROM databasechangelog WHERE id = ?", String.class, CHANGESET);
    }

    @Test
    void itBuildsAValidPartialIndexOnceAndRollsBack() throws Exception {
        liquibase(false);

        assertThat(index()).isEqualTo("valid");
        assertThat(db.queryForObject("SELECT pg_get_indexdef('idx_outbox_unfinished'::regclass)", String.class))
            .contains("ON public.outbox USING btree (submission_serial_number, outbox_id)")
            .contains("'pending'", "'failed'", "'dead'");

        liquibase(false);
        assertThat(runs()).as("a second update runs nothing").containsExactly("EXECUTED");

        liquibase(true);
        assertThat(index()).isNull();
        assertThat(runs()).isEmpty();

        liquibase(false);
        assertThat(index()).isEqualTo("valid");
    }

    @Test
    void itReplacesAnInvalidIndexLeftByAFailedConcurrentBuild() throws Exception {
        db.execute("CREATE INDEX idx_outbox_unfinished ON outbox (outbox_id)");
        db.update("UPDATE pg_index SET indisvalid = false WHERE indexrelid = 'idx_outbox_unfinished'::regclass");
        assertThat(index()).isEqualTo("invalid");

        liquibase(false);

        assertThat(index()).isEqualTo("valid");
        assertThat(db.queryForObject("SELECT pg_get_indexdef('idx_outbox_unfinished'::regclass)", String.class))
            .contains("(submission_serial_number, outbox_id)");
        assertThat(runs()).containsExactly("EXECUTED");
    }

    @Test
    void itOnlyMarksItselfRanWhenAValidIndexIsAlreadyThere() throws Exception {
        db.execute("""
            CREATE INDEX idx_outbox_unfinished ON outbox (submission_serial_number, outbox_id)
            WHERE status IN ('pending', 'failed', 'dead')""");

        liquibase(false);

        assertThat(index()).isEqualTo("valid");
        assertThat(runs()).containsExactly("MARK_RAN");
    }
}
