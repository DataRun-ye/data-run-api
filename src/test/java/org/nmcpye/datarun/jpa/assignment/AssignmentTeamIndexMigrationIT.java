package org.nmcpye.datarun.jpa.assignment;

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
 * The assignment team index changeset on its own, against PostgreSQL: it builds a valid index once, rolls back,
 * fails after its lock timeout instead of waiting behind a writer, replaces an invalid leftover, and only marks
 * itself ran when a valid index is already there.
 */
class AssignmentTeamIndexMigrationIT {

    private static final String CHANGELOG = "config/liquibase/changelog/assignment/20261125-assignment-team-index.xml";
    private static final String CHANGESET = "20261125-02-assignment-team-index";
    private static final String DEFINITION =
        "CREATE INDEX idx_assignment_team_id ON public.assignment USING btree (team_id)";

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");

    private static JdbcTemplate db;

    @BeforeAll
    static void startDatabase() {
        PG.start();
        db = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        db.execute("""
            CREATE TABLE assignment (
              id varchar(26) CONSTRAINT pk_assignment PRIMARY KEY,
              activity_id varchar(26) NOT NULL,
              org_unit_id varchar(26) NOT NULL,
              team_id varchar(26) NOT NULL,
              CONSTRAINT uc_assignment_activity_id UNIQUE (activity_id, org_unit_id, team_id))""");
        db.update("""
            INSERT INTO assignment (id, activity_id, org_unit_id, team_id)
            SELECT 'a' || i, 'activity' || (i % 3), 'orgunit' || i, 'team' || (i % 50)
            FROM generate_series(1, 5000) AS i""");
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    @BeforeEach
    void startFromNoIndexAndNoHistory() {
        db.execute("DROP INDEX IF EXISTS idx_assignment_team_id");
        db.execute("DROP TABLE IF EXISTS databasechangelog");
        db.execute("DROP TABLE IF EXISTS databasechangeloglock");
    }

    @Test
    void itBuildsAValidIndexOnceAndRollsBack() throws Exception {
        liquibase(false);

        assertThat(index()).isEqualTo("valid");
        assertThat(definition()).isEqualTo(DEFINITION);

        liquibase(false);
        assertThat(runs()).as("a second update runs nothing").containsExactly("EXECUTED");

        liquibase(true);
        assertThat(index()).isNull();
        assertThat(runs()).isEmpty();

        liquibase(false);
        assertThat(index()).isEqualTo("valid");
    }

    /** An admin's open write holds a lock the build needs: the migration fails visibly instead of waiting. */
    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void aHeldWriteLockMakesTheMigrationFailAfterItsLockTimeoutInsteadOfWaiting() throws Exception {
        try (Connection writer = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            writer.setAutoCommit(false);
            try (Statement statement = writer.createStatement()) {
                statement.execute("LOCK TABLE assignment IN ROW EXCLUSIVE MODE");
            }
            long start = System.nanoTime();
            assertThatThrownBy(() -> liquibase(false, "2s")).hasStackTraceContaining("lock timeout");
            assertThat((System.nanoTime() - start) / 1e9).isBetween(1.5, 20.0);
            writer.rollback();
        }

        assertThat(index()).isNull();
        assertThat(runs()).isEmpty();
        liquibase(false);
        assertThat(index()).isEqualTo("valid");
    }

    /** An invalid leftover (an interrupted concurrent build) is not taken as done: it is dropped and built again. */
    @Test
    void anInvalidLeftoverIsDroppedAndTheIndexBuiltNormally() throws Exception {
        db.execute("CREATE INDEX idx_assignment_team_id ON assignment (activity_id)");
        db.update("UPDATE pg_index SET indisvalid = false WHERE indexrelid = 'idx_assignment_team_id'::regclass");
        assertThat(index()).isEqualTo("invalid");

        liquibase(false);

        assertThat(index()).isEqualTo("valid");
        assertThat(definition()).isEqualTo(DEFINITION);
        assertThat(runs()).containsExactly("EXECUTED");
    }

    /** Where the index was made by hand, valid, this changeset only marks itself ran. */
    @Test
    void itOnlyMarksItselfRanWhenAValidIndexIsAlreadyThere() throws Exception {
        db.execute("CREATE INDEX idx_assignment_team_id ON assignment (team_id)");

        liquibase(false);

        assertThat(index()).isEqualTo("valid");
        assertThat(runs()).containsExactly("MARK_RAN");
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
                liquibase.setChangeLogParameter("assignmentTeamIndexLockTimeout", lockTimeout);
            }
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
            WHERE c.relname = 'idx_assignment_team_id'""", Boolean.class);
        return valid.isEmpty() ? null : valid.get(0) ? "valid" : "invalid";
    }

    private static String definition() {
        return db.queryForObject("SELECT pg_get_indexdef('idx_assignment_team_id'::regclass)", String.class);
    }

    private static List<String> runs() {
        return db.queryForList("SELECT exectype FROM databasechangelog WHERE id = ?", String.class, CHANGESET);
    }
}
