package org.nmcpye.datarun.config;

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
 * The error_event purge on its own, against PostgreSQL: it empties the table once and keeps it, its rollback
 * restores nothing and fails nothing (6.5.4 writes new rows into the empty table), and it fails after its lock
 * timeout instead of waiting behind a reader.
 */
class ErrorEventPurgeMigrationIT {

    private static final String CHANGELOG = "config/liquibase/changelog/error-event/20261125-purge-error-event.xml";
    private static final String CHANGESET = "20261125-04-purge-error-event";

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");

    private static JdbcTemplate db;

    @BeforeAll
    static void startDatabase() {
        PG.start();
        db = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        db.execute("""
            CREATE TABLE error_event (
              id bigserial CONSTRAINT pk_error_event PRIMARY KEY,
              occurred_at timestamp,
              level varchar(255),
              status integer,
              path varchar(255),
              method varchar(255),
              username varchar(255),
              client_ip varchar(255),
              message text,
              exception varchar(255),
              stacktrace text,
              context_json text)""");
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    @BeforeEach
    void startFromOldRowsAndNoHistory() {
        db.execute("DROP TABLE IF EXISTS databasechangelog");
        db.execute("DROP TABLE IF EXISTS databasechangeloglock");
        db.execute("TRUNCATE error_event");
        db.update("""
            INSERT INTO error_event (occurred_at, level, status, message)
            SELECT now(), 'ERROR', 500, 'an old raw message ' || i FROM generate_series(1, 100) AS i""");
    }

    @Test
    void itEmptiesTheTableOnceAndItsRollbackRestoresNothing() throws Exception {
        liquibase(false);
        assertThat(rows()).isZero();

        db.update("INSERT INTO error_event (occurred_at, level, status) VALUES (now(), 'ERROR', 500)");
        liquibase(false);
        assertThat(runs()).as("it runs once").containsExactly("EXECUTED");
        assertThat(rows()).as("a row 6.5.4 writes after the purge stays").isEqualTo(1);

        liquibase(true);
        assertThat(runs()).isEmpty();
        assertThat(rows()).as("nothing comes back, nothing else goes").isEqualTo(1);
        db.update("INSERT INTO error_event (occurred_at, level, status) VALUES (now(), 'ERROR', 500)");
        assertThat(rows()).as("6.5.4 can still write the table").isEqualTo(2);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void aReaderMakesThePurgeFailAfterItsLockTimeoutInsteadOfWaiting() throws Exception {
        try (Connection reader = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            reader.setAutoCommit(false);
            try (Statement statement = reader.createStatement()) {
                statement.execute("LOCK TABLE error_event IN ACCESS SHARE MODE");
            }
            long start = System.nanoTime();
            assertThatThrownBy(() -> liquibase(false, "2s")).hasStackTraceContaining("lock timeout");
            assertThat((System.nanoTime() - start) / 1e9).isBetween(1.5, 20.0);
            reader.rollback();
        }

        assertThat(rows()).as("nothing purged").isEqualTo(100);
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
                liquibase.setChangeLogParameter("errorEventPurgeLockTimeout", lockTimeout);
            }
            if (rollback) {
                liquibase.rollback(1, (String) null);
            } else {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    private static int rows() {
        Integer rows = db.queryForObject("SELECT count(*) FROM error_event", Integer.class);
        return rows == null ? -1 : rows;
    }

    private static List<String> runs() {
        return db.queryForList("SELECT exectype FROM databasechangelog WHERE id = ?", String.class, CHANGESET);
    }
}
