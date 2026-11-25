package org.nmcpye.datarun.jpa.datasubmission.conflict;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The conflict changeset on its own, against PostgreSQL, with the two tables it references reduced to their keys: it
 * adds schema submission_review and its conflict table, keeps one OPEN conflict per submission and uploader, checks
 * states, causes and decisions, rolls back while empty, and refuses to roll back once a conflict exists.
 */
@Timeout(60)
class ConflictTableMigrationIT {

    private static final String CHANGELOG =
        "config/liquibase/changelog/submission_review/20261125-submission-review-conflict.xml";
    private static final String CHANGESET = "20261125-s2-02-submission-review-conflict";

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
        db.execute("DROP SCHEMA IF EXISTS submission_review CASCADE");
        db.execute("DROP TABLE IF EXISTS databasechangelog");
        db.execute("DROP TABLE IF EXISTS databasechangeloglock");
        db.execute("DROP TABLE IF EXISTS data_submission");
        db.execute("DROP TABLE IF EXISTS app_user");
        db.execute("CREATE TABLE data_submission (id varchar(26) PRIMARY KEY)");
        db.execute("CREATE TABLE app_user (id varchar(26) PRIMARY KEY)");
        db.execute("INSERT INTO data_submission VALUES ('S1'), ('S2')");
        db.execute("INSERT INTO app_user VALUES ('U1'), ('U2'), ('R1')");
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
            "SELECT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'submission_review')", Boolean.class));
    }

    private static List<String> runs() {
        return db.queryForList("SELECT exectype FROM databasechangelog WHERE id = ?", String.class, CHANGESET);
    }

    /** An OPEN conflict of the given submission and uploader, as detection opens one. */
    private static void open(String submission, String uploader) {
        db.update("""
            INSERT INTO submission_review.conflict (id, submission_id, uploader_user_id, state, cause, base_version,
                                                    rows_lack_ids, upload_route, phone_copy, first_received_at,
                                                    last_received_at)
            VALUES (gen_random_uuid()::text, ?, ?, 'OPEN', 'SERVER_SIDE_CHANGE', 0, false, 'VERSIONED', '{}', now(),
                    now())""", submission, uploader);
    }

    @Test
    void itAddsTheTableOnceAndRollsBackWhileEmpty() throws Exception {
        liquibase(false);

        assertThat(db.queryForList("""
                SELECT column_name || ' ' || data_type || ' ' || is_nullable FROM information_schema.columns
                WHERE table_schema = 'submission_review' AND table_name = 'conflict' ORDER BY ordinal_position""",
            String.class))
            .containsExactly(
                "id character varying NO",
                "submission_id character varying NO",
                "uploader_user_id character varying NO",
                "state character varying NO",
                "cause character varying NO",
                "base_version bigint YES",
                "rows_lack_ids boolean NO",
                "upload_route character varying NO",
                "phone_copy jsonb NO",
                "first_received_at timestamp with time zone NO",
                "last_received_at timestamp with time zone NO",
                "resend_count integer NO",
                "version bigint NO",
                "decided_at timestamp with time zone YES",
                "decided_by_user_id character varying YES",
                "decision_reason text YES",
                "reapplied_edits boolean YES",
                "accepted_version bigint YES");
        assertThat(db.queryForObject("SELECT pg_get_indexdef('submission_review.uq_conflict_open'::regclass)",
            String.class)).contains("UNIQUE INDEX").contains("(submission_id, uploader_user_id)")
            .contains("WHERE ((state)::text = 'OPEN'::text)");

        liquibase(false);
        assertThat(runs()).as("a second update runs nothing").containsExactly("EXECUTED");

        liquibase(true);
        assertThat(schemaExists()).isFalse();
        assertThat(runs()).isEmpty();
    }

    @Test
    void oneOpenConflictPerSubmissionAndUploader() throws Exception {
        liquibase(false);
        open("S1", "U1");
        open("S1", "U2");
        open("S2", "U1");

        assertThatThrownBy(() -> open("S1", "U1")).hasStackTraceContaining("uq_conflict_open");

        db.update("""
            UPDATE submission_review.conflict SET state = 'KEPT', decided_at = now(), decided_by_user_id = 'R1',
                                                  decision_reason = 'nothing to take'
            WHERE submission_id = 'S1' AND uploader_user_id = 'U1'""");
        open("S1", "U1");
        assertThat(db.queryForObject("""
            SELECT count(*) FROM submission_review.conflict WHERE submission_id = 'S1' AND uploader_user_id = 'U1'""",
            Integer.class)).as("a kept conflict and a new open one").isEqualTo(2);
    }

    @Test
    void statesCausesBasesAndDecisionsAreChecked() throws Exception {
        liquibase(false);
        String insert = """
            INSERT INTO submission_review.conflict (id, submission_id, uploader_user_id, state, cause, base_version,
                                                    rows_lack_ids, upload_route, phone_copy, first_received_at,
                                                    last_received_at, decided_at, decided_by_user_id, decision_reason,
                                                    reapplied_edits, accepted_version)
            VALUES (gen_random_uuid()::text, 'S1', 'U1', %s, %s, %s, false, %s, '{}', now(), now(), %s)""";
        String undecided = "NULL, NULL, NULL, NULL, NULL";

        db.update(insert.formatted("'OPEN'", "'DETECTION_ERROR'", "NULL", "'LEGACY'", undecided));
        db.update(insert.formatted("'KEPT'", "'SERVER_SIDE_CHANGE'", "3", "'VERSIONED'",
            "now(), 'R1', 'nothing to take', NULL, NULL"));
        db.update(insert.formatted("'ACCEPTED'", "'SERVER_SIDE_CHANGE'", "3", "'VERSIONED'",
            "now(), 'R1', 'the phone was right', false, 5"));

        assertThatThrownBy(() -> db.update(insert.formatted("'HELD'", "'SERVER_SIDE_CHANGE'", "3", "'VERSIONED'",
            undecided))).hasStackTraceContaining("ck_conflict_state");
        assertThatThrownBy(() -> db.update(insert.formatted("'OPEN'", "'CHANGED'", "3", "'VERSIONED'", undecided)))
            .hasStackTraceContaining("ck_conflict_cause");
        assertThatThrownBy(() -> db.update(insert.formatted("'OPEN'", "'SERVER_SIDE_CHANGE'", "NULL", "'VERSIONED'",
            undecided))).as("detection always knows the base").hasStackTraceContaining("ck_conflict_base");
        assertThatThrownBy(() -> db.update(insert.formatted("'OPEN'", "'SERVER_SIDE_CHANGE'", "3", "'BULK'",
            undecided))).hasStackTraceContaining("ck_conflict_upload_route");
        assertThatThrownBy(() -> db.update(insert.formatted("'OPEN'", "'SERVER_SIDE_CHANGE'", "3", "'VERSIONED'",
            "now(), 'R1', 'early', NULL, NULL"))).as("an open conflict has no decision")
            .hasStackTraceContaining("ck_conflict_decision");
        assertThatThrownBy(() -> db.update(insert.formatted("'KEPT'", "'SERVER_SIDE_CHANGE'", "3", "'VERSIONED'",
            "now(), 'R1', NULL, NULL, NULL"))).as("a decision has a reason")
            .hasStackTraceContaining("ck_conflict_decision");
        assertThatThrownBy(() -> db.update(insert.formatted("'ACCEPTED'", "'SERVER_SIDE_CHANGE'", "3", "'VERSIONED'",
            "now(), 'R1', 'the phone was right', false, NULL"))).as("an accept names the version it made")
            .hasStackTraceContaining("ck_conflict_decision");
        assertThatThrownBy(() -> db.update(insert.formatted("'OPEN'", "'SERVER_SIDE_CHANGE'", "3", "'VERSIONED'",
            undecided).replace("'S1'", "'NOSUCH'"))).hasStackTraceContaining("fk_conflict_submission");
    }

    /** A conflict holds a phone copy its phone was told was saved: the rollback refuses and leaves it all in place. */
    @Test
    void theRollbackRefusesOnceAConflictExists() throws Exception {
        liquibase(false);
        open("S1", "U1");

        assertThatThrownBy(() -> liquibase(true)).hasStackTraceContaining("never roll it back");

        assertThat(db.queryForObject("SELECT count(*) FROM submission_review.conflict", Integer.class)).isEqualTo(1);
        assertThat(runs()).containsExactly("EXECUTED");
    }
}
