package org.nmcpye.datarun.submission.feed;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The grants on schema feed converge to downstream's reader role at start, against PostgreSQL 16: a role named and
 * existing reads the feed and nothing more, and no other role keeps anything on it; naming another role moves the
 * grant; with no role, a role that does not exist or a name that is not plain, nothing changes; a name is quoted;
 * feed_ops is never touched.
 */
class FeedReaderGrantsIT {

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");
    private static final List<String> ROLES = List.of("feed_reader", "other_reader", "Feed-Reader");

    private static JdbcTemplate db;
    private static FeedReaderGrants grants;

    private final Logger logger = (Logger) LoggerFactory.getLogger(FeedReaderGrants.class);
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();

    @BeforeAll
    static void startDatabase() {
        PG.start();
        DriverManagerDataSource dataSource =
            new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        db = new JdbcTemplate(dataSource);
        grants = new FeedReaderGrants(new NamedParameterJdbcTemplate(dataSource),
            new DataSourceTransactionManager(dataSource), new MockEnvironment());
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    @BeforeEach
    void freshSchemaAndRoles() throws Exception {
        db.execute("DROP SCHEMA IF EXISTS feed CASCADE");
        db.execute("DROP SCHEMA IF EXISTS feed_ops CASCADE");
        db.execute("DROP TABLE IF EXISTS databasechangelog");
        db.execute("DROP TABLE IF EXISTS databasechangeloglock");
        for (String role : ROLES) {
            db.execute("""
                DO $$ BEGIN
                    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '%1$s') THEN
                        EXECUTE format('DROP OWNED BY %%I', '%1$s');
                        EXECUTE format('DROP ROLE %%I', '%1$s');
                    END IF;
                END $$""".formatted(role));
        }
        ChangeLogHistoryServiceFactory.getInstance().resetAll();
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase(FeedSchemaMigrationIT.CHANGELOG, new ClassLoaderResourceAccessor(), database)
                .update(new Contexts(), new LabelExpression());
        }
        db.execute("CREATE ROLE feed_reader");
        db.execute("CREATE ROLE other_reader");
        lines.start();
        logger.addAppender(lines);
    }

    @AfterEach
    void release() {
        logger.detachAppender(lines);
    }

    /** What a role may do in the two schemas, one line per privilege, PUBLIC included. */
    private static List<String> privileges(String role) {
        return db.queryForList("""
            SELECT 'USAGE ' || n.nspname FROM pg_namespace n
            WHERE n.nspname IN ('feed', 'feed_ops') AND has_schema_privilege(?, n.oid, 'USAGE')
            UNION ALL
            SELECT 'CREATE ' || n.nspname FROM pg_namespace n
            WHERE n.nspname IN ('feed', 'feed_ops') AND has_schema_privilege(?, n.oid, 'CREATE')
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
            ORDER BY 1""", String.class, role, role, role, role);
    }

    /** Who holds what in schema feed's default privileges, one line each. */
    private static List<String> defaults() {
        return db.queryForList("""
            SELECT d.defaclobjtype::text || ' ' || CASE WHEN a.grantee = 0 THEN 'PUBLIC' ELSE a.grantee::regrole::text END
                   || ' ' || a.privilege_type
            FROM pg_default_acl d JOIN pg_namespace n ON n.oid = d.defaclnamespace
            CROSS JOIN LATERAL aclexplode(d.defaclacl) a
            WHERE n.nspname = 'feed' AND a.grantee <> d.defaclrole
            ORDER BY 1""", String.class);
    }

    private List<String> logged() {
        return lines.list.stream().map(line -> line.getLevel() + " " + line.getFormattedMessage()).toList();
    }

    private static final List<String> READS_THE_FEED = List.of(
        "SELECT feed.form_field",
        "SELECT feed.form_section",
        "SELECT feed.freshness",
        "SELECT feed.submission",
        "SELECT feed.submission_row",
        "USAGE feed");

    @Test
    void aRoleNamedThatExistsReadsTheFeedAndLaterStreams_andNothingMore() {
        grants.converge(Optional.of("feed_reader"));

        assertThat(privileges("feed_reader")).containsExactlyElementsOf(READS_THE_FEED);
        assertThat(defaults()).containsExactly("r feed_reader SELECT");
        db.execute("CREATE TABLE feed.later_stream (id text)");
        db.execute("CREATE TABLE feed_ops.later_bookkeeping (id text)");
        assertThat(privileges("feed_reader")).contains("SELECT feed.later_stream")
            .doesNotContain("SELECT feed_ops.later_bookkeeping", "USAGE feed_ops", "SEQUENCE feed.position_seq");
        assertThat(logged()).containsExactly(
            "INFO Schema feed is readable by reader role feed_reader, and by no other role (7 grants made or revoked)");

        grants.converge(Optional.of("feed_reader"));
        assertThat(logged()).last().isEqualTo(
            "INFO Schema feed is readable by reader role feed_reader, and by no other role (0 grants made or revoked)");
    }

    @Test
    void everyOtherGranteeLosesWhatItHoldsOnTheFeed_andTheReaderAnyMore_butFeedOpsIsLeftAlone() {
        db.execute("GRANT USAGE, CREATE ON SCHEMA feed TO other_reader");
        db.execute("GRANT SELECT, INSERT ON feed.submission TO other_reader");
        db.execute("GRANT USAGE ON SEQUENCE feed.position_seq TO other_reader, feed_reader");
        db.execute("GRANT SELECT ON feed.form_field TO PUBLIC");
        db.execute("GRANT INSERT ON feed.freshness TO feed_reader");
        db.execute("GRANT CREATE ON SCHEMA feed TO feed_reader");
        db.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA feed GRANT SELECT ON TABLES TO other_reader");
        db.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA feed GRANT INSERT ON TABLES TO feed_reader");
        db.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA feed GRANT USAGE ON SEQUENCES TO PUBLIC");
        db.execute("GRANT USAGE ON SCHEMA feed_ops TO other_reader");

        grants.converge(Optional.of("feed_reader"));

        assertThat(privileges("feed_reader")).containsExactlyElementsOf(READS_THE_FEED);
        assertThat(privileges("other_reader")).as("only what it holds on feed_ops").containsExactly("USAGE feed_ops");
        assertThat(db.queryForObject("""
                SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                CROSS JOIN LATERAL aclexplode(c.relacl) a
                WHERE n.nspname = 'feed' AND a.grantee = 0""", Long.class)).as("nothing for PUBLIC").isZero();
        assertThat(defaults()).containsExactly("r feed_reader SELECT");
    }

    @Test
    void namingAnotherRoleMovesTheGrant() {
        grants.converge(Optional.of("feed_reader"));
        grants.converge(Optional.of("other_reader"));

        assertThat(privileges("other_reader")).containsExactlyElementsOf(READS_THE_FEED);
        assertThat(privileges("feed_reader")).isEmpty();
        db.execute("CREATE TABLE feed.later_stream (id text)");
        assertThat(privileges("feed_reader")).as("no default privilege left either").isEmpty();
        assertThat(privileges("other_reader")).contains("SELECT feed.later_stream");
    }

    @Test
    void withoutARoleNothingChanges_soALostSettingCutsNoOneOff() {
        grants.converge(Optional.of("feed_reader"));
        lines.list.clear();

        grants.converge(Optional.empty());

        assertThat(privileges("feed_reader")).containsExactlyElementsOf(READS_THE_FEED);
        assertThat(logged()).containsExactly("WARN Downstream's grant on schema feed is not made: no reader role is set "
            + "(datarun.feed.reader-role); grants on feed are left as they are");
    }

    @Test
    void aRoleThatDoesNotExistChangesNothing() {
        grants.converge(Optional.of("feed_reader"));
        lines.list.clear();

        grants.converge(Optional.of("not_a_role"));

        assertThat(privileges("feed_reader")).containsExactlyElementsOf(READS_THE_FEED);
        assertThat(logged()).containsExactly("WARN Downstream's grant on schema feed is not made: reader role "
            + "not_a_role does not exist; grants on feed are left as they are");
    }

    @Test
    void aNameThatIsNotAPlainRoleNameChangesNothing() {
        grants.converge(Optional.of("feed_reader"));
        lines.list.clear();

        grants.converge(Optional.of("feed_reader\"; DROP SCHEMA feed CASCADE; --"));
        grants.converge(Optional.of("feed_reader; REVOKE ALL ON SCHEMA feed FROM feed_reader"));

        assertThat(db.queryForObject("SELECT to_regclass('feed.submission') IS NOT NULL", Boolean.class)).isTrue();
        assertThat(privileges("feed_reader")).containsExactlyElementsOf(READS_THE_FEED);
        assertThat(logged()).hasSize(2).allMatch(line -> line.startsWith("WARN ")
            && line.contains("datarun.feed.reader-role is not a plain role name"));
    }

    /** A plain name that needs quoting, mixed case and a hyphen, is granted to that exact role. */
    @Test
    void aNameIsQuotedAsTheDatabaseQuotesIt() {
        db.execute("CREATE ROLE \"Feed-Reader\"");

        grants.converge(Optional.of("Feed-Reader"));

        assertThat(privileges("Feed-Reader")).containsExactlyElementsOf(READS_THE_FEED);
        assertThat(defaults()).containsExactly("r \"Feed-Reader\" SELECT");
    }

    @Test
    void aFailureIsLoggedAndNeverThrown() {
        db.execute("DROP SCHEMA feed CASCADE");

        grants.converge(Optional.of("feed_reader"));

        assertThat(logged()).containsExactly("WARN Downstream's grant on schema feed is not made: schema feed does not "
            + "exist");
    }
}
