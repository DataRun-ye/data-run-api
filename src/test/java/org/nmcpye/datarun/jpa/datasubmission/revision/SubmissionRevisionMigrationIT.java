package org.nmcpye.datarun.jpa.datasubmission.revision;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.FastCheckService;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The submission change record's migrations against PostgreSQL, as production meets them: a fresh database is
 * migrated up to step C's first changeset, given submissions and outbox rows the way 6.5.4 leaves them, and then
 * migrated through step C. The tests then read what the migrations built and what the database refuses.
 */
class SubmissionRevisionMigrationIT {

    private static final String MASTER = "config/liquibase/master.xml";
    /** Every changeset of step C starts with this id. */
    static final String STEP_C = "20261124-c";

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");

    private static JdbcTemplate db;
    private static NamedParameterJdbcTemplate named;

    @BeforeAll
    static void migrateThroughStepC() throws Exception {
        PG.start();
        DriverManagerDataSource dataSource =
            new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        db = new JdbcTemplate(dataSource);
        named = new NamedParameterJdbcTemplate(dataSource);
        migrate(true);
        assertThat(db.queryForObject("SELECT to_regclass('public.submission_revision') IS NULL", Boolean.class))
            .as("the first phase stops before step C").isTrue();
        migrate(false);
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    /**
     * One Liquibase run of the whole changelog, as a fresh process makes it. With {@code beforeStepC}, only the
     * changesets that come before step C's first one.
     */
    static void migrate(boolean beforeStepC) throws Exception {
        ChangeLogHistoryServiceFactory.getInstance().resetAll();
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(MASTER, new ClassLoaderResourceAccessor(), database);
            if (beforeStepC) {
                List<ChangeSet> unrun = liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression());
                int first = 0;
                while (first < unrun.size() && !unrun.get(first).getId().startsWith(STEP_C)) {
                    first++;
                }
                assertThat(first).as("step C's changesets are in the changelog").isLessThan(unrun.size());
                liquibase.update(first, new Contexts(), new LabelExpression());
            } else {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    /** A stored submission, as JPA writes it (UTC times), with a fresh id; returns the id. */
    static String submission(long lockVersion, boolean deleted) {
        String id = ulid();
        named.update("""
                INSERT INTO data_submission (id, uid, template_uid, template_version_uid, template_version_no,
                                             assignment_uid, activity_uid, team_uid, team_code, org_unit_uid,
                                             org_unit_code, org_unit_name, status, start_entry_time,
                                             finished_entry_time, form_data, lock_version, created_by, created_date,
                                             last_modified_by, last_modified_date, deleted, deleted_at)
                VALUES (:id, :uid, 'migForm0001', 'migVers0001', 2, 'migAssign01', 'migActiv001', 'migTeam0001',
                        'T1', 'migOrgUn001', 'OU1', 'a village', 'COMPLETED',
                        TIMESTAMP '2026-11-20 05:00:00.25', TIMESTAMP '2026-11-20 05:10:00',
                        CAST('{"main": {"name": "first"}}' AS jsonb), :lockVersion, 'mig-test',
                        TIMESTAMP '2026-11-21 08:00:00.123456', 'mig-test', TIMESTAMP '2026-11-22 09:30:00.5',
                        :deleted, CASE WHEN :deleted THEN TIMESTAMP '2026-11-22 09:30:00.5' END)""",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("uid", id.substring(15))
                .addValue("lockVersion", lockVersion)
                .addValue("deleted", deleted));
        return id;
    }

    /** A user who can act, with a fresh id; returns the id. */
    static String user() {
        String id = ulid();
        named.update("""
                INSERT INTO app_user (id, uid, login, password_hash, activated, lang_key, created_by)
                VALUES (:id, :uid, :login, 'x', true, 'en', 'mig-test')""",
            Map.of("id", id, "uid", id.substring(15), "login", "mig-" + id.toLowerCase()));
        return id;
    }

    static String ulid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 26).toUpperCase();
    }

    /** Inserts a revision whose state is the submission's row as it is now. */
    static void revision(String submissionId, long version, Long previousVersion, String kind, String command,
                         String actorUserId, String actorSystem, String onBehalfOf, String reason) {
        named.update("""
                INSERT INTO submission_revision (submission_id, version, previous_version, kind, command,
                                                 actor_user_id, actor_system, on_behalf_of_user_id, reason,
                                                 form_data, metadata)
                SELECT s.id, :version, :previousVersion, :kind, :command, :actorUserId, :actorSystem, :onBehalfOf,
                       :reason, s.form_data, submission_revision_metadata(s)
                FROM data_submission s WHERE s.id = :id""",
            new MapSqlParameterSource()
                .addValue("id", submissionId)
                .addValue("version", version)
                .addValue("previousVersion", previousVersion)
                .addValue("kind", kind)
                .addValue("command", command)
                .addValue("actorUserId", actorUserId)
                .addValue("actorSystem", actorSystem)
                .addValue("onBehalfOf", onBehalfOf)
                .addValue("reason", reason));
    }

    /** The SQLSTATE and constraint the database refused {@code work} with. */
    static String refusal(Runnable work) {
        try {
            work.run();
        } catch (DataAccessException e) {
            Throwable cause = e;
            while (cause != null && !(cause instanceof SQLException)) {
                cause = cause.getCause();
            }
            assertThat(cause).as("a database refusal").isNotNull();
            SQLException refusal = (SQLException) cause;
            String constraint = refusal instanceof org.postgresql.util.PSQLException psql
                && psql.getServerErrorMessage() != null ? psql.getServerErrorMessage().getConstraint() : null;
            return refusal.getSQLState() + (constraint == null ? "" : " " + constraint);
        }
        throw new AssertionError("the database accepted it");
    }

    // ---------------------------------------------------------------- the table and its constraints

    @Test
    void theMetadataIsWhatAWriteCanChange() {
        String id = submission(3, true);

        Map<String, Object> metadata = db.queryForMap("""
            SELECT m ->> 'template_version_uid' AS template_version_uid,
                   m ->> 'template_version_no' AS template_version_no, m ->> 'org_unit_name' AS org_unit_name,
                   m ->> 'status' AS status, m ->> 'deleted' AS deleted, m ->> 'deleted_at' AS deleted_at,
                   m ->> 'last_modified_date' AS last_modified_date,
                   (SELECT array_agg(k ORDER BY k) FROM jsonb_object_keys(m) AS k)::text AS keys
            FROM (SELECT submission_revision_metadata(s) AS m FROM data_submission s WHERE s.id = ?) x""", id);

        assertThat(metadata)
            .containsEntry("template_version_uid", "migVers0001")
            .containsEntry("template_version_no", "2")
            .containsEntry("org_unit_name", "a village")
            .containsEntry("status", "COMPLETED")
            .containsEntry("deleted", "true")
            .containsEntry("deleted_at", "2026-11-22T09:30:00.500000Z")
            .containsEntry("last_modified_date", "2026-11-22T09:30:00.500000Z")
            .containsEntry("keys", "{activity_uid,assignment_uid,deleted,deleted_at,last_modified_date,org_unit_code,"
                + "org_unit_name,org_unit_uid,status,team_code,team_uid,template_version_no,template_version_uid}");
    }

    /** The rendering of times does not depend on the session's time zone. */
    @Test
    void theMetadataIsTheSameInEverySessionTimeZone() throws Exception {
        String id = submission(0, false);
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("SET TimeZone = 'Asia/Aden'");
            var aden = statement.executeQuery(
                "SELECT submission_revision_metadata(s)::text FROM data_submission s WHERE s.id = '" + id + "'");
            aden.next();
            String inAden = aden.getString(1);
            statement.execute("SET TimeZone = 'UTC'");
            var utc = statement.executeQuery(
                "SELECT submission_revision_metadata(s)::text FROM data_submission s WHERE s.id = '" + id + "'");
            utc.next();
            assertThat(inAden).isEqualTo(utc.getString(1))
                .contains("\"last_modified_date\": \"2026-11-22T09:30:00.500000Z\"");
        }
    }

    @Test
    void aRevisionOfEachKindIsAcceptedWithItsActor() {
        String admin = user();
        String uploader = user();
        String created = submission(0, false);
        String baselined = submission(4, false);

        revision(created, 0, null, "CREATE", "HELD_RELEASE", admin, null, uploader, "area went live");
        revision(created, 1, 0L, "UPDATE", "UPLOAD", uploader, null, null, null);
        revision(created, 2, 1L, "DELETE", "ADMIN_DELETE", admin, null, null, null);
        revision(baselined, 4, null, "BASELINE", "MIGRATION", null, "MIGRATION", null, null);
        revision(baselined, 7, 4L, "BASELINE", "GAP_REPAIR", null, "MIGRATION", null, null);

        assertThat(db.queryForList("""
                SELECT version || ' ' || kind || ' ' || command || ' ' || coalesce(previous_version::text, '-')
                FROM submission_revision WHERE submission_id IN (?, ?) ORDER BY submission_id = ?, version""",
            String.class, created, baselined, baselined))
            .containsExactly("0 CREATE HELD_RELEASE -", "1 UPDATE UPLOAD 0", "2 DELETE ADMIN_DELETE 1",
                "4 BASELINE MIGRATION -", "7 BASELINE GAP_REPAIR 4");
        assertThat(db.queryForObject("""
                SELECT recorded_at BETWEEN now() - interval '1 minute' AND now()
                FROM submission_revision WHERE submission_id = ? AND version = 0""", Boolean.class, created))
            .as("recorded_at is the database's time").isTrue();
    }

    @Test
    void aVersionIsRecordedOnce() {
        String uploader = user();
        String id = submission(0, false);
        revision(id, 0, null, "CREATE", "UPLOAD", uploader, null, null, null);

        assertThat(refusal(() -> revision(id, 0, null, "CREATE", "UPLOAD", uploader, null, null, null)))
            .isEqualTo("23505 pk_submission_revision");
    }

    @Test
    void aRevisionNamesAStoredSubmissionAndKnownUsers() {
        String uploader = user();
        String id = submission(0, false);

        assertThat(refusal(() -> named.update("""
                INSERT INTO submission_revision (submission_id, version, kind, command, actor_user_id, metadata)
                VALUES ('01NOSUCHSUBMISSION00000000', 0, 'CREATE', 'UPLOAD', :actor, '{}')""",
            Map.of("actor", uploader))))
            .isEqualTo("23503 fk_submission_revision_submission");
        assertThat(refusal(() -> revision(id, 0, null, "CREATE", "UPLOAD", "01NOSUCHUSER00000000000000", null, null,
            null)))
            .isEqualTo("23503 fk_submission_revision_actor");
        assertThat(refusal(() -> revision(id, 0, null, "CREATE", "HELD_RELEASE", uploader, null,
            "01NOSUCHUSER00000000000000", "note")))
            .isEqualTo("23503 fk_submission_revision_on_behalf_of");
    }

    @Test
    void everyRevisionHasExactlyOneActorOfTheRightKind() {
        String user = user();
        String id = submission(0, false);

        assertThat(refusal(() -> revision(id, 0, null, "CREATE", "UPLOAD", null, null, null, null)))
            .as("no actor").isEqualTo("23514 ck_submission_revision_actor");
        assertThat(refusal(() -> revision(id, 0, null, "CREATE", "UPLOAD", user, "MIGRATION", null, null)))
            .as("two actors").isEqualTo("23514 ck_submission_revision_actor");
        assertThat(refusal(() -> revision(id, 0, null, "BASELINE", "MIGRATION", user, null, null, null)))
            .as("a user on a system command").isEqualTo("23514 ck_submission_revision_system");
        assertThat(refusal(() -> revision(id, 0, null, "BASELINE", "MIGRATION", null, "SCRIPT", null, null)))
            .as("an unknown system").isEqualTo("23514 ck_submission_revision_system");
        assertThat(refusal(() -> revision(id, 0, null, "CREATE", "HELD_RELEASE", user, null, null, null)))
            .as("a release without its uploader").isEqualTo("23514 ck_submission_revision_on_behalf_of");
        assertThat(refusal(() -> revision(id, 0, null, "CREATE", "UPLOAD", user, null, user, null)))
            .as("on behalf of, outside a release").isEqualTo("23514 ck_submission_revision_on_behalf_of");
    }

    @Test
    void eachKindTakesItsOwnVersionsAndCommands() {
        String user = user();
        String id = submission(5, false);

        assertThat(refusal(() -> revision(id, 1, null, "CREATE", "UPLOAD", user, null, null, null)))
            .as("a creation above 0").isEqualTo("23514 ck_submission_revision_version");
        assertThat(refusal(() -> revision(id, 5, 3L, "UPDATE", "UPLOAD", user, null, null, null)))
            .as("an update that skips a version").isEqualTo("23514 ck_submission_revision_version");
        assertThat(refusal(() -> revision(id, 5, 5L, "BASELINE", "GAP_REPAIR", null, "MIGRATION", null, null)))
            .as("a baseline that does not move forward").isEqualTo("23514 ck_submission_revision_version");
        assertThat(refusal(() -> revision(id, 5, 4L, "UPDATE", "MIGRATION", null, "MIGRATION", null, null)))
            .as("a system command writes only baselines").isEqualTo("23514 ck_submission_revision_baseline");
        assertThat(refusal(() -> revision(id, 5, null, "BASELINE", "UPLOAD", user, null, null, null)))
            .as("a baseline comes from a system command").isEqualTo("23514 ck_submission_revision_baseline");
        assertThat(refusal(() -> revision(id, 5, 4L, "RESTORE", "UPLOAD", user, null, null, null)))
            .as("an unknown kind").isEqualTo("23514 ck_submission_revision_kind");
    }

    // ---------------------------------------------------------------- the outbox

    @Test
    void theOutboxNamesAVersionAndTakesItsTimeFromTheDatabaseInUtc() throws Exception {
        String id = submission(0, false);
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("SET TimeZone = 'Asia/Aden'");
            statement.execute("""
                INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload,
                                    event_type, status, attempt, submission_version)
                SELECT id, serial_number, uid, template_version_uid, form_data, 'SAVE', 'pending', 0, lock_version
                FROM data_submission WHERE id = '%s'""".formatted(id));
        }
        Map<String, Object> row = db.queryForMap("""
            SELECT submission_version,
                   abs(extract(epoch FROM (timezone('UTC', now()) - created_at))) < 60 AS utc
            FROM outbox WHERE submission_id = ?""", id);
        assertThat(row).containsEntry("submission_version", 0L).containsEntry("utc", true);
        assertThat(db.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_name = 'outbox' AND column_name = 'submission_version'""", String.class))
            .as("rows written before step C keep NULL").isEqualTo("YES");
    }
}
