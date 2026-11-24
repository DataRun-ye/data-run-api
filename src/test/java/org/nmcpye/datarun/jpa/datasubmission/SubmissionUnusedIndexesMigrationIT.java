package org.nmcpye.datarun.jpa.datasubmission;

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
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The changeset that drops the GIN on data_submission.form_data and the duplicate serial_number index, on its own
 * against PostgreSQL: it drops exactly those two and keeps the others, runs once, rolls back to the same
 * definitions, and fails after its lock timeout instead of waiting behind a long read.
 */
class SubmissionUnusedIndexesMigrationIT {

    private static final String CHANGELOG =
        "config/liquibase/changelog/data_submission/20261125-drop-unused-submission-indexes.xml";
    private static final String CHANGESET = "20261125-03-drop-unused-submission-indexes";

    private static final List<String> KEPT = List.of(
        "data_submission_pkey", "idx_ds_active_form_assignment", "idx_ds_form_assignment", "idx_ds_orgunit",
        "uk_data_submission_serial", "uk_data_submission_uid");

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");

    private static JdbcTemplate db;

    @BeforeAll
    static void startDatabase() {
        PG.start();
        db = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        db.execute("""
            CREATE TABLE data_submission (
              id varchar(26) CONSTRAINT data_submission_pkey PRIMARY KEY,
              serial_number bigserial NOT NULL,
              uid varchar(11) NOT NULL,
              template_uid varchar(255),
              template_version_uid varchar(255),
              assignment_uid varchar(64) NOT NULL,
              org_unit_uid varchar(64) NOT NULL,
              deleted boolean DEFAULT false,
              form_data jsonb DEFAULT '{}'::jsonb,
              CONSTRAINT uk_data_submission_serial UNIQUE (serial_number),
              CONSTRAINT uk_data_submission_uid UNIQUE (uid))""");
        db.update("""
            INSERT INTO data_submission (id, uid, template_uid, template_version_uid, assignment_uid, org_unit_uid, form_data)
            SELECT 'id' || i, 'u' || i, 'form' || (i % 5), 'version' || (i % 5), 'asg' || (i % 40), 'ou' || (i % 40),
                   jsonb_build_object('n', i)
            FROM generate_series(1, 2000) AS i""");
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    /** The indexes as production has them (changeset 2025-09-01), and no history. */
    @BeforeEach
    void startFromProductionsIndexesAndNoHistory() {
        db.execute("DROP TABLE IF EXISTS databasechangelog");
        db.execute("DROP TABLE IF EXISTS databasechangeloglock");
        for (String index : List.of(
            "CREATE INDEX IF NOT EXISTS idx_ds_form_assignment ON data_submission (template_version_uid, assignment_uid)",
            "CREATE INDEX IF NOT EXISTS idx_ds_orgunit ON data_submission (org_unit_uid)",
            "CREATE INDEX IF NOT EXISTS idx_ds_serial_number ON data_submission (serial_number)",
            "CREATE INDEX IF NOT EXISTS idx_ds_active_form_assignment ON data_submission (template_uid, assignment_uid) "
                + "WHERE (deleted = false)",
            "CREATE INDEX IF NOT EXISTS idx_ds_form_data_gin ON data_submission USING gin (form_data)")) {
            db.execute(index);
        }
    }

    @Test
    void itDropsTheGinAndTheDuplicateOnceAndRollsBackToTheirDefinitions() throws Exception {
        Map<String, String> before = indexes();
        assertThat(before).hasSize(8);

        liquibase(false);
        assertThat(indexes()).containsOnlyKeys(KEPT);
        assertThat(indexes()).allSatisfy((name, definition) -> assertThat(definition).isEqualTo(before.get(name)));

        liquibase(false);
        assertThat(runs()).as("a second update runs nothing").containsExactly("EXECUTED");

        liquibase(true);
        assertThat(indexes()).as("the rollback restores each definition").isEqualTo(before);
        assertThat(runs()).isEmpty();
    }

    /** A long read holds the table: the migration fails visibly instead of waiting behind it. */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void aLongReadMakesTheMigrationFailAfterItsLockTimeoutInsteadOfWaiting() throws Exception {
        try (Connection reader = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            reader.setAutoCommit(false);
            try (Statement statement = reader.createStatement()) {
                statement.execute("LOCK TABLE data_submission IN ACCESS SHARE MODE");
            }
            long start = System.nanoTime();
            assertThatThrownBy(() -> liquibase(false, "2s")).hasStackTraceContaining("lock timeout");
            assertThat((System.nanoTime() - start) / 1e9).isBetween(1.5, 20.0);
            reader.rollback();
        }

        assertThat(indexes()).as("nothing dropped").hasSize(8);
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
                liquibase.setChangeLogParameter("submissionIndexLockTimeout", lockTimeout);
            }
            if (rollback) {
                liquibase.rollback(1, (String) null);
            } else {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    /** Each index of the table, by name, with its definition. */
    private static Map<String, String> indexes() {
        Map<String, String> indexes = new TreeMap<>();
        db.queryForList("""
                SELECT c.relname AS name, pg_get_indexdef(c.oid) AS definition
                FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid
                WHERE i.indrelid = 'public.data_submission'::regclass""")
            .forEach(row -> indexes.put((String) row.get("name"), (String) row.get("definition")));
        return indexes;
    }

    private static List<String> runs() {
        return db.queryForList("SELECT exectype FROM databasechangelog WHERE id = ?", String.class, CHANGESET);
    }
}
