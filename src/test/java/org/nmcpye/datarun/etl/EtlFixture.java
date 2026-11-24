package org.nmcpye.datarun.etl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Field definitions, submissions and outbox rows for ETL tests, written with plain SQL in committed
 * transactions (the pool does not auto-commit). Uids are at most 11 characters: the tall table's
 * uid columns are varchar(11).
 */
final class EtlFixture {

    static final String TEMPLATE = "etlTmpl0001";
    static final String TEMPLATE_VERSION = "etlVers0001";

    /** Paths of the field definitions, as the template generator writes them: sections, then a repeat. */
    static final List<String> ROOT_PATHS = List.of("main.hh_name", "main.hh_size", "main.has_nets", "main.visit_note");
    static final String REPEAT_PATH = "members";
    static final List<String> MEMBER_PATHS = List.of("members.member_name", "members.member_age", "members.sleeps_under_net");

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;

    EtlFixture(NamedParameterJdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transactionManager);
    }

    static UUID elementId(String templateUid, String path) {
        return UUID.nameUUIDFromBytes((templateUid + "|" + path).getBytes(StandardCharsets.UTF_8));
    }

    /** Root fields in a section, a repeatable section and the fields inside it. */
    void defineTemplate(String templateUid) {
        tx.executeWithoutResult(status -> {
            for (String path : ROOT_PATHS) {
                element(templateUid, path, path.endsWith("hh_size") ? "INTEGER" : "TEXT", null, null);
            }
            UUID repeat = element(templateUid, REPEAT_PATH, "ARRAY", "Repeat", null);
            for (String path : MEMBER_PATHS) {
                element(templateUid, path, path.endsWith("age") ? "INTEGER" : "TEXT", null, repeat);
            }
        });
    }

    /**
     * A template with one field definition whose semantic type the application does not know. Loading the
     * template's field definitions then fails in Java while the database transaction stays healthy: the shape
     * of any failure raised outside the database.
     */
    void defineTemplateTheApplicationCannotRead(String templateUid) {
        tx.executeWithoutResult(status -> {
            for (String path : ROOT_PATHS) {
                element(templateUid, path, "TEXT", null, null);
            }
            element(templateUid, "main.unknown", "TEXT", "NotASemanticType", null);
        });
    }

    private UUID element(String templateUid, String path, String dataType, String semanticType, UUID parentRepeat) {
        UUID id = elementId(templateUid, path);
        String safeName = path.replace('.', '_');
        jdbc.update("""
                INSERT INTO canonical_element (id, template_uid, preferred_name, safe_name, data_type, semantic_type,
                                               canonical_path, json_data_paths, parent_repeat_id, created_date)
                VALUES (:id, :template, :name, :safeName, :dataType, :semanticType, :path,
                        CAST(:paths AS jsonb), :parent, now())
                ON CONFLICT (id) DO NOTHING
                """,
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("template", templateUid)
                .addValue("name", safeName)
                .addValue("safeName", safeName)
                .addValue("dataType", dataType)
                .addValue("semanticType", semanticType)
                .addValue("path", path)
                .addValue("paths", "[\"" + path + "\"]")
                .addValue("parent", parentRepeat == null ? null : parentRepeat.toString()));
        return id;
    }

    /** A submission with four root values and {@code members} repeat rows of four values each. */
    long submission(String uid, String templateUid, int members) {
        return submissionWithFormData(uid, templateUid, formData(uid, members));
    }

    /** A submission whose form data is given as JSON. */
    long submissionWithFormData(String uid, String templateUid, String formData) {
        return tx.execute(status -> jdbc.queryForObject("""
                INSERT INTO data_submission (id, uid, template_uid, template_version_uid, template_version_no,
                                             assignment_uid, activity_uid, team_uid, org_unit_uid, status,
                                             form_data, lock_version, created_by, created_date,
                                             last_modified_by, last_modified_date, deleted)
                VALUES (:id, :uid, :template, :version, 1, 'etlAssign01', 'etlActiv001', 'etlTeam0001',
                        'etlOrgUn001', NULL, CAST(:formData AS jsonb), 0, 'etl-test', now(),
                        'etl-test', now(), false)
                RETURNING serial_number
                """,
            new MapSqlParameterSource()
                .addValue("id", submissionId(uid))
                .addValue("uid", uid)
                .addValue("template", templateUid)
                .addValue("version", TEMPLATE_VERSION)
                .addValue("formData", formData),
            Long.class));
    }

    static String submissionId(String uid) {
        return ("01ETLTEST" + uid + "00000000000000000").substring(0, 26).toUpperCase();
    }

    static String formData(String uid, int members) {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < members; i++) {
            if (i > 0) rows.append(',');
            rows.append("""
                {"_id":"%s","_index":%d,"member_name":"member %d","member_age":%d,"sleeps_under_net":"yes"}"""
                .formatted(uid + "m" + i, i + 1, i + 1, 20 + i));
        }
        return """
            {"main":{"hh_name":"household %s","hh_size":%d,"has_nets":"yes","visit_note":"first visit"},"members":[%s]}"""
            .formatted(uid, members, rows);
    }

    /** An outbox row for a stored submission, as the upload path writes it. */
    long outbox(String uid, String eventType) {
        return tx.execute(status -> jdbc.queryForObject("""
                INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload,
                                    event_type, status, attempt, created_at)
                SELECT id, serial_number, uid, template_version_uid, form_data, :eventType, 'pending', 0, now()
                FROM data_submission WHERE uid = :uid
                RETURNING outbox_id
                """,
            new MapSqlParameterSource().addValue("uid", uid).addValue("eventType", eventType),
            Long.class));
    }

    /** An outbox row whose submission does not exist (removed outside the application). */
    long outboxWithoutSubmission(String uid, long serial) {
        return tx.execute(status -> jdbc.queryForObject("""
                INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload,
                                    event_type, status, attempt, created_at)
                VALUES (:id, :serial, :uid, :version, '{}'::jsonb, 'SAVE', 'pending', 0, now())
                RETURNING outbox_id
                """,
            new MapSqlParameterSource()
                .addValue("id", submissionId(uid))
                .addValue("serial", serial)
                .addValue("uid", uid)
                .addValue("version", TEMPLATE_VERSION),
            Long.class));
    }

    Map<String, Object> outboxRow(long outboxId) {
        return jdbc.queryForMap("SELECT * FROM outbox WHERE outbox_id = :id", Map.of("id", outboxId));
    }

    /** Whether the row is held back until a time still to come (a claim's lease or a retry time). */
    boolean isLeased(long outboxId) {
        return leaseSecondsLeft(outboxId) > 0;
    }

    double leaseSecondsLeft(long outboxId) {
        Double left = jdbc.queryForObject(
            "SELECT extract(epoch FROM next_attempt_at - localtimestamp) FROM outbox WHERE outbox_id = :id",
            Map.of("id", outboxId), Double.class);
        return left == null ? 0 : left;
    }

    /** An outbox row of no particular submission, with the status, age and retry time given (in minutes). */
    long outboxRow(String status, int createdMinutesAgo, Integer retryInMinutes) {
        return tx.execute(txStatus -> jdbc.queryForObject("""
                INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload,
                                    event_type, status, attempt, created_at, next_attempt_at)
                VALUES ('etl-row', 1, 'etlRow00001', :version, '{}'::jsonb, 'SAVE', :status, 0,
                        localtimestamp - make_interval(mins => :ago),
                        localtimestamp + make_interval(mins => :retryIn))
                RETURNING outbox_id
                """,
            new MapSqlParameterSource()
                .addValue("version", TEMPLATE_VERSION)
                .addValue("status", status)
                .addValue("ago", createdMinutesAgo)
                .addValue("retryIn", retryInMinutes),
            Long.class));
    }

    /** Sets how often the row was already claimed, as runs that died on it would leave it. */
    void setAttempt(long outboxId, int attempt) {
        tx.executeWithoutResult(status -> jdbc.update(
            "UPDATE outbox SET attempt = :attempt WHERE outbox_id = :id", Map.of("id", outboxId, "attempt", attempt)));
    }

    /** Ends the row's lease or retry wait, as time passing would. */
    void endLease(long outboxId) {
        tx.executeWithoutResult(status -> jdbc.update(
            "UPDATE outbox SET next_attempt_at = localtimestamp - interval '1 second' WHERE outbox_id = :id",
            Map.of("id", outboxId)));
    }

    List<Map<String, Object>> auditRows(long outboxId) {
        return jdbc.queryForList(
            "SELECT * FROM outbox_processing WHERE outbox_id = :id ORDER BY id", Map.of("id", outboxId));
    }

    List<Map<String, Object>> runs() {
        return jdbc.queryForList("SELECT * FROM etl_runs ORDER BY started_at", Map.of());
    }

    int tallRows(String submissionUid) {
        return jdbc.queryForObject("SELECT count(*) FROM analytics.tall_canonical WHERE submission_uid = :uid",
            Map.of("uid", submissionUid), Integer.class);
    }

    List<Map<String, Object>> events(String submissionUid) {
        return jdbc.queryForList("SELECT * FROM analytics.events WHERE submission_uid = :uid ORDER BY event_id",
            Map.of("uid", submissionUid));
    }

    /** Makes every tall-row write for a submission whose uid starts with {@code fail} raise an error. */
    void failProjectionsOfFailUids() {
        tx.executeWithoutResult(status -> {
            JdbcTemplate plain = jdbc.getJdbcTemplate();
            plain.execute("""
                CREATE OR REPLACE FUNCTION etl_test_fail() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                  IF NEW.submission_uid LIKE 'fail%' THEN
                    RAISE EXCEPTION 'injected projection failure';
                  END IF;
                  RETURN NEW;
                END $$""");
            plain.execute("DROP TRIGGER IF EXISTS etl_test_fail ON analytics.tall_canonical");
            plain.execute("""
                CREATE TRIGGER etl_test_fail BEFORE INSERT OR UPDATE ON analytics.tall_canonical
                FOR EACH ROW EXECUTE FUNCTION etl_test_fail()""");
        });
    }

    /** Removes everything the ETL reads or writes; the database is shared by every integration test. */
    void clean() {
        tx.executeWithoutResult(status -> {
            JdbcTemplate plain = jdbc.getJdbcTemplate();
            plain.execute("DROP TRIGGER IF EXISTS etl_test_fail ON analytics.tall_canonical");
            plain.execute("DROP FUNCTION IF EXISTS etl_test_fail()");
            plain.update("DELETE FROM outbox_processing");
            plain.update("DELETE FROM outbox");
            plain.update("DELETE FROM etl_runs");
            plain.update("DELETE FROM analytics.tall_canonical");
            plain.update("DELETE FROM analytics.events");
            plain.update("DELETE FROM data_submission WHERE created_by = 'etl-test'");
            plain.update("DELETE FROM canonical_element WHERE template_uid LIKE 'etl%'");
        });
    }
}
