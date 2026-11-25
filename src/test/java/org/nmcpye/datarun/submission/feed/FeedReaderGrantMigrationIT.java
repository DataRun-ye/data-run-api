package org.nmcpye.datarun.submission.feed;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Downstream's grant on schema feed, against PostgreSQL 16, with and without a reader role: nothing is granted or
 * recorded while no existing role is named; the first run with an existing role grants it read access to feed, and to
 * streams added later, and nothing on feed_ops; naming another role later fails nothing and grants nothing; the
 * startup check reads the real catalog; the rollback revokes.
 */
class FeedReaderGrantMigrationIT {

    private static final String GRANT_CHANGELOG = "config/liquibase/changelog/feed/20261125-feed-reader-grant.xml";

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");

    private static JdbcTemplate db;

    private final Logger checkLogger = (Logger) LoggerFactory.getLogger(FeedReaderGrantCheck.class);
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();

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
        for (String role : List.of("feed_reader", "other_reader")) {
            db.execute("""
                DO $$ BEGIN
                    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '%1$s') THEN
                        EXECUTE 'DROP OWNED BY %1$s';
                        EXECUTE 'DROP ROLE %1$s';
                    END IF;
                END $$""".formatted(role));
        }
        lines.list.clear();
        lines.start();
        checkLogger.addAppender(lines);
    }

    /** One Liquibase run of a changelog, as a fresh process would make it, with the reader role's parameter. */
    private static void liquibase(String changelog, String readerRole, boolean rollback) throws Exception {
        ChangeLogHistoryServiceFactory.getInstance().resetAll();
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
            if (readerRole != null) {
                liquibase.setChangeLogParameter(FeedReaderRole.LIQUIBASE_PARAMETER, readerRole);
            }
            if (rollback) {
                liquibase.rollback(1, (String) null);
            } else {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    /** The schema, then the grant, as one start makes them. */
    private static void start(String readerRole) throws Exception {
        liquibase(FeedSchemaMigrationIT.CHANGELOG, readerRole, false);
        liquibase(GRANT_CHANGELOG, readerRole, false);
    }

    private static List<String> recorded() {
        return db.queryForList("SELECT id FROM databasechangelog ORDER BY orderexecuted", String.class);
    }

    /** What a role may do in the feed's schemas, one line per privilege it has. */
    private static List<String> privileges(String role) {
        return db.queryForList("""
            SELECT 'USAGE ' || n.nspname FROM pg_namespace n
            WHERE n.nspname IN ('feed', 'feed_ops') AND has_schema_privilege(?, n.oid, 'USAGE')
            UNION ALL
            SELECT p.privilege || ' ' || n.nspname || '.' || c.relname
            FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
            CROSS JOIN (VALUES ('SELECT'), ('INSERT'), ('UPDATE'), ('DELETE'), ('TRUNCATE')) AS p(privilege)
            WHERE n.nspname IN ('feed', 'feed_ops') AND c.relkind = 'r' AND has_table_privilege(?, c.oid, p.privilege)
            UNION ALL
            SELECT 'SEQUENCE ' || n.nspname || '.' || c.relname
            FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE CASE WHEN n.nspname IN ('feed', 'feed_ops') AND c.relkind = 'S'
                       THEN has_sequence_privilege(?, c.oid, 'USAGE, SELECT, UPDATE') ELSE false END
            ORDER BY 1""", String.class, role, role, role);
    }

    private List<String> checkLines() {
        return lines.list.stream().map(line -> line.getLevel() + " " + line.getFormattedMessage()).toList();
    }

    private void checkAtStart(String readerRole) {
        MockEnvironment environment = new MockEnvironment();
        if (readerRole != null) {
            environment.setProperty(FeedReaderRole.PROPERTY, readerRole);
        }
        new FeedReaderGrantCheck(new NamedParameterJdbcTemplate(db), environment).checkAtStart();
    }

    @Test
    void withoutAReaderRoleNothingIsGrantedNorRecorded_andTheStartSaysSo() throws Exception {
        start(null);
        start("");
        assertThat(recorded()).as("the grant is skipped, never recorded, so it is tried again at each start")
            .containsExactly(FeedSchemaMigrationIT.CHANGESET);

        checkAtStart(null);
        assertThat(checkLines()).containsExactly(
            "WARN Downstream's grant on schema feed is not made: no reader role is set (datarun.feed.reader-role)");

        db.execute("CREATE ROLE feed_reader");
        assertThat(privileges("feed_reader")).as("a role created later has nothing until a start names it").isEmpty();
    }

    @Test
    void aNamedRoleThatDoesNotExistYetIsGrantedAtTheFirstStartAfterItIsCreated() throws Exception {
        start("feed_reader");
        assertThat(recorded()).containsExactly(FeedSchemaMigrationIT.CHANGESET);
        checkAtStart("feed_reader");

        db.execute("CREATE ROLE feed_reader");
        checkAtStart("feed_reader");
        start("feed_reader");
        checkAtStart("feed_reader");

        assertThat(checkLines()).containsExactly(
            "WARN Downstream's grant on schema feed is not made: reader role feed_reader does not exist; the first "
                + "start after it is created makes it",
            "WARN Downstream's grant on schema feed is not made yet for reader role feed_reader, created after this "
                + "start's migrations: the next start makes it",
            "INFO Schema feed is readable by reader role feed_reader");
        assertThat(recorded()).containsExactly(FeedSchemaMigrationIT.CHANGESET, FeedReaderGrantCheck.GRANT_CHANGESET);
    }

    @Test
    void theRoleReadsEveryStreamAndLaterOnes_writesNothing_andGetsNothingOnFeedOps() throws Exception {
        db.execute("CREATE ROLE feed_reader");
        start("feed_reader");

        assertThat(privileges("feed_reader")).containsExactly(
            "SELECT feed.form_field",
            "SELECT feed.form_section",
            "SELECT feed.freshness",
            "SELECT feed.submission",
            "SELECT feed.submission_row",
            "USAGE feed");

        // a stream added later by the schema's owner, as a later changeset would add it
        db.execute("CREATE TABLE feed.later_stream (id text)");
        db.execute("CREATE TABLE feed_ops.later_bookkeeping (id text)");
        assertThat(privileges("feed_reader")).contains("SELECT feed.later_stream")
            .doesNotContain("SELECT feed_ops.later_bookkeeping", "USAGE feed_ops");
    }

    @Test
    void namingAnotherRoleLaterFailsNothingAndGrantsItNothing() throws Exception {
        db.execute("CREATE ROLE feed_reader");
        db.execute("CREATE ROLE other_reader");
        start("feed_reader");

        start("other_reader");
        checkAtStart("other_reader");

        assertThat(recorded()).containsExactly(FeedSchemaMigrationIT.CHANGESET, FeedReaderGrantCheck.GRANT_CHANGESET);
        assertThat(privileges("other_reader")).isEmpty();
        assertThat(checkLines()).containsExactly("WARN Reader role other_reader cannot read schema feed, and changeset "
            + "20261125-d02-feed-reader-grant already ran (for this role or another): grant it USAGE on feed, SELECT on "
            + "its tables and default privileges by hand");
    }

    @Test
    void theRollbackRevokesTheGrant() throws Exception {
        db.execute("CREATE ROLE feed_reader");
        start("feed_reader");

        liquibase(GRANT_CHANGELOG, "feed_reader", true);

        assertThat(privileges("feed_reader")).isEmpty();
        db.execute("CREATE TABLE feed.later_stream (id text)");
        assertThat(privileges("feed_reader")).as("no default privilege left").isEmpty();
        assertThat(recorded()).containsExactly(FeedSchemaMigrationIT.CHANGESET);
        db.execute("DROP TABLE feed.later_stream");

        start("feed_reader");
        assertThat(privileges("feed_reader")).contains("USAGE feed");
    }

    @Test
    void aNameThatIsNotAPlainRoleNameIsPassedAsNoName_andTheStartSaysSo() throws Exception {
        db.execute("CREATE ROLE feed_reader");
        MockEnvironment environment = new MockEnvironment()
            .withProperty(FeedReaderRole.PROPERTY, "feed_reader\"; DROP SCHEMA feed CASCADE; --");

        start(FeedReaderRole.liquibaseParameter(environment));
        new FeedReaderGrantCheck(new NamedParameterJdbcTemplate(db), environment).checkAtStart();

        assertThat(recorded()).containsExactly(FeedSchemaMigrationIT.CHANGESET);
        assertThat(privileges("feed_reader")).isEmpty();
        assertThat(db.queryForObject("SELECT to_regclass('feed.submission') IS NOT NULL", Boolean.class)).isTrue();
        assertThat(checkLines()).containsExactly("WARN Downstream's grant on schema feed is not made: "
            + "datarun.feed.reader-role is not a plain role name (letters, digits, _ and -, at most 63 characters)");
        assertThat(lines.list).noneMatch(line -> line.getLevel() == Level.ERROR);
    }
}
