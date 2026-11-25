package org.nmcpye.datarun.submission.feed.publisher;

import org.nmcpye.datarun.jpa.datasubmission.revision.SubmissionTestPurge;
import org.nmcpye.datarun.submission.normalized.DerivedIds;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration, submissions and changes for the publisher's tests, written with plain SQL the way the application
 * writes them: one campaign placement, one form with two versions (a plain section, a repeating section and a nested
 * one), and changes that each write the submission's row, its revision in the journal and its outbox row in one
 * transaction, as {@code SubmissionCommandService}'s persistence layer does. A change can be held open and committed
 * later, as a slow writer's would be. Ids, uids, names and values are invented.
 */
final class FeedFixture {

    static final String FORM_UID = "fdForm00001";
    static final String FORM_ID = "01FDFORM000000000000000001";
    static final String VERSION_1_UID = "fdFormVer01";
    static final String VERSION_1_ID = "01FDFORMVER000000000000001";
    static final String VERSION_2_UID = "fdFormVer02";
    static final String VERSION_2_ID = "01FDFORMVER000000000000002";

    static final String ACTIVITY_ID = "fd-activity";
    static final String ACTIVITY_UID = "fdActivity1";
    static final String ORG_UNIT_ID = "fd-org-unit";
    static final String ORG_UNIT_UID = "fdOrgUnit01";
    static final String TEAM_ID = "fd-team";
    static final String TEAM_UID = "fdTeam00001";
    static final String ASSIGNMENT_ID = "fd-assignment";
    static final String ASSIGNMENT_UID = "fdAssign001";
    /** An assignment uid that names no assignment: a placement the feed cannot publish. */
    static final String MISSING_ASSIGNMENT_UID = "fdAssignNo1";
    static final String ACTOR_ID = "fd-actor";

    static final String NAME_FIELD = "01FDDE00000000000000000001";
    static final String COUNT_FIELD = "01FDDE00000000000000000002";
    static final String ITEM_FIELD = "01FDDE00000000000000000003";
    static final String PART_FIELD = "01FDDE00000000000000000004";

    static final String ROWS_SECTION = DerivedIds.sectionId(FORM_ID, "main.rows");
    static final String PARTS_SECTION = DerivedIds.sectionId(FORM_ID, "main.rows.parts");

    /** Every submission the fixture writes has an id with this prefix and a uid starting with {@code fd}. */
    static final String ID_PREFIX = "01FDSUB";

    private static final AtomicInteger NEXT = new AtomicInteger();

    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final String url;
    private final String username;
    private final String password;

    FeedFixture(NamedParameterJdbcTemplate jdbc, PlatformTransactionManager transactionManager, String url,
                String username, String password) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transactionManager);
        this.url = url;
        this.username = username;
        this.password = password;
    }

    /** A new submission's id and uid. */
    record Ids(String id, String uid) {
    }

    static Ids nextIds() {
        int n = NEXT.incrementAndGet();
        String uid = String.format("fd%09d", n);
        String id = (ID_PREFIX + String.format("%019d", n)).substring(0, 26);
        return new Ids(id, uid);
    }

    /** The placement, the actor, the data elements and the form's two versions, once per database. */
    void ensureConfiguration() {
        tx.executeWithoutResult(status -> {
            UUID rows = canonicalElement("main.rows", "ARRAY", "Repeat", null);
            canonicalElement("main.fd_name", "TEXT", null, null);
            canonicalElement("main.fd_count", "INTEGER", null, null);
            canonicalElement("main.rows.fd_item", "TEXT", null, rows);
        });
        Integer present = jdbc.queryForObject("SELECT count(*) FROM data_template WHERE uid = :uid",
            Map.of("uid", FORM_UID), Integer.class);
        if (present != null && present > 0) {
            return;
        }
        tx.executeWithoutResult(status -> {
            update("INSERT INTO project (id, name, created_by) VALUES ('fd-project', 'fd project', 'fd-test')");
            update("""
                INSERT INTO activity (id, uid, name, project_id, disabled, created_by)
                VALUES ('fd-activity', 'fdActivity1', 'fd activity', 'fd-project', false, 'fd-test')""");
            update("""
                INSERT INTO org_unit (id, uid, code, name, path, parent_id, created_by)
                VALUES ('fd-org-unit', 'fdOrgUnit01', 'FDOU', 'fd org unit', ',fdOrgUnit01', NULL, 'fd-test')""");
            update("""
                INSERT INTO team (id, uid, code, name, activity_id, disabled, form_permissions, created_by)
                VALUES ('fd-team', 'fdTeam00001', 'FDT', 'fd team', 'fd-activity', false, '[]'::jsonb, 'fd-test')""");
            update("""
                INSERT INTO assignment (id, uid, team_id, activity_id, org_unit_id, forms, deleted, created_by)
                VALUES ('fd-assignment', 'fdAssign001', 'fd-team', 'fd-activity', 'fd-org-unit',
                        '["fdForm00001"]'::jsonb, false, 'fd-test')""");
            update("""
                INSERT INTO app_user (id, uid, login, password_hash, activated, lang_key, created_by)
                VALUES ('fd-actor', 'fdActor0001', 'fd-actor', 'x', true, 'en', 'fd-test')""");
            dataElement(NAME_FIELD, "fdDeName001", "fd_name", "Text");
            dataElement(COUNT_FIELD, "fdDeCount01", "fd_count", "Integer");
            dataElement(ITEM_FIELD, "fdDeItem001", "fd_item", "Text");
            dataElement(PART_FIELD, "fdDePart001", "fd_part", "Text");
            jdbc.update("""
                    INSERT INTO data_template (id, uid, code, name, version_uid, version_number, created_by)
                    VALUES (:id, :uid, :uid, 'fd form', :version, 2, 'fd-test')""",
                Map.of("id", FORM_ID, "uid", FORM_UID, "version", VERSION_2_UID));
            String sections = """
                [{"id": "main", "name": "main", "path": "main", "repeatable": false},
                 {"id": "rows", "name": "rows", "path": "main.rows", "parent": "main", "repeatable": true},
                 {"id": "parts", "name": "parts", "path": "main.rows.parts", "parent": "rows", "repeatable": true}]""";
            version(VERSION_1_ID, VERSION_1_UID, 1, sections, """
                [{"id": "fdDeName001", "name": "fd_name", "path": "main.fdDeName001", "type": "Text"},
                 {"id": "fdDeItem001", "name": "fd_item", "path": "main.rows.fdDeItem001", "type": "Text"},
                 {"id": "fdDePart001", "name": "fd_part", "path": "main.rows.parts.fdDePart001", "type": "Text"}]""");
            version(VERSION_2_ID, VERSION_2_UID, 2, sections, """
                [{"id": "fdDeName001", "name": "fd_name", "path": "main.fdDeName001", "type": "Text"},
                 {"id": "fdDeCount01", "name": "fd_count", "path": "main.fdDeCount01", "type": "Integer"},
                 {"id": "fdDeItem001", "name": "fd_item", "path": "main.rows.fdDeItem001", "type": "Text"},
                 {"id": "fdDePart001", "name": "fd_part", "path": "main.rows.parts.fdDePart001", "type": "Text"}]""");
        });
    }

    /** A field definition of the ETL's, so that tall and events have something to write for the form. */
    private UUID canonicalElement(String path, String dataType, String semanticType, UUID parentRepeat) {
        UUID id = UUID.nameUUIDFromBytes((FORM_UID + "|" + path).getBytes(StandardCharsets.UTF_8));
        String safeName = path.replace('.', '_');
        jdbc.update("""
                INSERT INTO canonical_element (id, template_uid, preferred_name, safe_name, data_type, semantic_type,
                                               canonical_path, json_data_paths, parent_repeat_id, created_date)
                VALUES (:id, :template, :name, :safeName, :dataType, :semanticType, :path, CAST(:paths AS jsonb),
                        :parent, now())
                ON CONFLICT (id) DO NOTHING""",
            new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id)
                .addValue("template", FORM_UID)
                .addValue("name", safeName)
                .addValue("safeName", safeName)
                .addValue("dataType", dataType)
                .addValue("semanticType", semanticType)
                .addValue("path", path)
                .addValue("paths", "[\"" + path + "\"]")
                .addValue("parent", parentRepeat == null ? null : parentRepeat.toString()));
        return id;
    }

    private void dataElement(String id, String uid, String name, String type) {
        jdbc.update("""
                INSERT INTO data_element (id, uid, code, name, type, created_by)
                VALUES (:id, :uid, :uid, :name, :type, 'fd-test')""",
            Map.of("id", id, "uid", uid, "name", name, "type", type));
    }

    private void version(String id, String uid, int number, String sections, String fields) {
        jdbc.update("""
                INSERT INTO data_template_version
                    (id, uid, template_uid, version_number, sections, fields, data_template_id, created_by)
                VALUES (:id, :uid, :formUid, :number, CAST(:sections AS jsonb), CAST(:fields AS jsonb), :formId,
                        'fd-test')""",
            Map.of("id", id, "uid", uid, "formUid", FORM_UID, "number", number, "sections", sections,
                "fields", fields, "formId", FORM_ID));
    }

    private void update(String sql) {
        jdbc.update(sql, Map.of());
    }

    /** A document of the form: a name, a count, and rows given as {@code id:item} with optional parts. */
    static String document(String name, int count, String... rows) {
        StringBuilder json = new StringBuilder();
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(rows[i]);
        }
        return """
            {"main": {"fd_name": "%s", "fd_count": %d, "rows": [%s]}}""".formatted(name, count, json);
    }

    /** One row of {@link #document}, with its parts given as {@code id:part}. */
    static String row(String id, String item, String... parts) {
        StringBuilder json = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String[] part = parts[i].split(":", 2);
            if (i > 0) {
                json.append(',');
            }
            json.append("""
                {"_id": "%s", "fd_part": "%s"}""".formatted(part[0], part[1]));
        }
        String nested = parts.length == 0 ? "" : ", \"parts\": [" + json + "]";
        return """
            {"_id": "%s", "fd_item": "%s"%s}""".formatted(id, item, nested);
    }

    /** One change in a transaction of its own: open until {@link #commit}, as a slow writer's would be. */
    final class Change implements AutoCloseable {
        private final Connection connection;

        private Change() throws SQLException {
            connection = DriverManager.getConnection(url, username, password);
            connection.setAutoCommit(false);
        }

        void commit() throws SQLException {
            connection.commit();
        }

        @Override
        public void close() throws SQLException {
            try {
                connection.rollback();
            } finally {
                connection.close();
            }
        }
    }

    /** Creates a submission with this document, pinned to version 2, committed. */
    Ids create(String document) {
        Ids ids = nextIds();
        create(ids, document, VERSION_2_UID, ASSIGNMENT_UID);
        return ids;
    }

    /** Creates a submission, committed. */
    void create(Ids ids, String document, String versionUid, String assignmentUid) {
        try (Change change = new Change()) {
            create(change, ids, document, versionUid, assignmentUid);
            change.commit();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Creates a submission in a change left open. */
    Change createOpen(Ids ids, String document) throws SQLException {
        Change change = new Change();
        create(change, ids, document, VERSION_2_UID, ASSIGNMENT_UID);
        return change;
    }

    private void create(Change change, Ids ids, String document, String versionUid, String assignmentUid)
        throws SQLException {
        try (PreparedStatement insert = change.connection.prepareStatement("""
            INSERT INTO data_submission (id, uid, template_uid, template_version_uid, template_version_no,
                                         assignment_uid, activity_uid, team_uid, org_unit_uid, status, form_data,
                                         lock_version, created_by, created_date, last_modified_by,
                                         last_modified_date, deleted, start_entry_time, finished_entry_time)
            VALUES (?, ?, ?, ?, 2, ?, ?, ?, ?, NULL, CAST(? AS jsonb), 0, 'fd-test', timezone('UTC', now()),
                    'fd-test', timezone('UTC', now()), false, timezone('UTC', now()) - interval '10 minutes',
                    timezone('UTC', now()) - interval '5 minutes')""")) {
            insert.setString(1, ids.id());
            insert.setString(2, ids.uid());
            insert.setString(3, FORM_UID);
            insert.setString(4, versionUid);
            insert.setString(5, assignmentUid);
            insert.setString(6, ACTIVITY_UID);
            insert.setString(7, TEAM_UID);
            insert.setString(8, ORG_UNIT_UID);
            insert.setString(9, document);
            insert.executeUpdate();
        }
        revisionAndOutbox(change, ids.id(), "CREATE", "SAVE");
    }

    /** Replaces the submission's document, committed: an upload that applies. */
    void update(Ids ids, String document) {
        try (Change change = updateOpen(ids, document)) {
            change.commit();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Replaces the submission's document in a change left open. */
    Change updateOpen(Ids ids, String document) throws SQLException {
        Change change = new Change();
        try (PreparedStatement update = change.connection.prepareStatement("""
            UPDATE data_submission SET form_data = CAST(? AS jsonb), lock_version = lock_version + 1,
                                       last_modified_date = timezone('UTC', now())
            WHERE id = ?""")) {
            update.setString(1, document);
            update.setString(2, ids.id());
            update.executeUpdate();
        }
        revisionAndOutbox(change, ids.id(), "UPDATE", "SAVE");
        return change;
    }

    /** Deletes the submission, committed: an admin delete. */
    void delete(Ids ids) {
        try (Change change = new Change()) {
            try (PreparedStatement update = change.connection.prepareStatement("""
                UPDATE data_submission SET deleted = true, deleted_at = timezone('UTC', now()),
                                           lock_version = lock_version + 1,
                                           last_modified_date = timezone('UTC', now())
                WHERE id = ?""")) {
                update.setString(1, ids.id());
                update.executeUpdate();
            }
            revisionAndOutbox(change, ids.id(), "DELETE", "DELETE");
            change.commit();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The revision of the submission's stored state and the outbox row naming it, as the persistence layer writes. */
    private static void revisionAndOutbox(Change change, String id, String kind, String eventType)
        throws SQLException {
        try (PreparedStatement revision = change.connection.prepareStatement("""
            INSERT INTO journal.submission_revision (submission_id, version, previous_version, kind, command,
                                                     actor_user_id, form_data, metadata)
            SELECT s.id, s.lock_version, CASE WHEN ? = 'CREATE' THEN NULL ELSE s.lock_version - 1 END, ?, 'UPLOAD',
                   'fd-actor', s.form_data, journal.submission_revision_metadata(s)
            FROM data_submission s WHERE s.id = ?""")) {
            revision.setString(1, kind);
            revision.setString(2, kind);
            revision.setString(3, id);
            revision.executeUpdate();
        }
        try (PreparedStatement outbox = change.connection.prepareStatement("""
            INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload, event_type,
                                status, attempt, submission_version)
            SELECT id, serial_number, uid, template_version_uid, form_data, ?, 'pending', 0, lock_version
            FROM data_submission WHERE id = ?""")) {
            outbox.setString(1, eventType);
            outbox.setString(2, id);
            outbox.executeUpdate();
        }
    }

    /** The submission's head version in the journal. */
    long head(Ids ids) {
        return jdbc.queryForObject("SELECT max(version) FROM journal.submission_revision WHERE submission_id = :id",
            Map.of("id", ids.id()), Long.class);
    }

    /** The feed's record of the submission, or an empty map. */
    Map<String, Object> record(Ids ids) {
        List<Map<String, Object>> records = jdbc.queryForList("""
                SELECT position, version, deleted, form_id, form_version_id, assignment_id, team_id, org_unit_id,
                       activity_id, field_values::text AS field_values, unmapped_values::text AS unmapped_values,
                       created_at, entered_start_at, entered_end_at, changed_at, published_at
                FROM feed.submission WHERE id = :id""",
            Map.of("id", ids.id()));
        return records.isEmpty() ? Map.of() : records.get(0);
    }

    /** The feed's row records of the submission, by row id. */
    List<Map<String, Object>> rows(Ids ids) {
        return jdbc.queryForList("""
                SELECT id, position, changed_in_version, deleted, id_origin, parent_row_id, section_id, row_index,
                       field_values::text AS field_values, unmapped_values::text AS unmapped_values
                FROM feed.submission_row WHERE submission_id = :id ORDER BY id""",
            Map.of("id", ids.id()));
    }

    /** The submission's pending change, or an empty map. */
    Map<String, Object> pending(Ids ids) {
        List<Map<String, Object>> pending = jdbc.queryForList(
            "SELECT attempts, reason, dead_at, next_attempt_at FROM feed_ops.pending WHERE submission_id = :id",
            Map.of("id", ids.id()));
        return pending.isEmpty() ? Map.of() : pending.get(0);
    }

    /** The status of the submission's outbox rows, in order. */
    List<String> outboxStatuses(Ids ids) {
        return jdbc.queryForList("SELECT status FROM outbox WHERE submission_id = :id ORDER BY outbox_id",
            Map.of("id", ids.id()), String.class);
    }

    long count(String sql, Map<String, ?> params) {
        Long n = jdbc.queryForObject(sql, params, Long.class);
        return n == null ? 0 : n;
    }

    /** Runs SQL in a committed transaction of its own. */
    void execute(String sql, Map<String, ?> params) {
        tx.executeWithoutResult(status -> jdbc.update(sql, params));
    }

    /** A connection of its own, outside the pool. */
    Connection connect() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /** Whether a query on a connection of its own returns at least one row. */
    boolean exists(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rows = statement.executeQuery()) {
            return rows.next();
        }
    }

    /**
     * Removes everything the publisher's tests read or write: the outbox and the ETL's bookkeeping (as the ETL's own
     * tests do), the feed's submission streams and freshness, feed_ops, and the fixture's submissions.
     */
    void clean() {
        tx.executeWithoutResult(status -> {
            update("DELETE FROM outbox_processing");
            update("DELETE FROM outbox");
            update("DELETE FROM etl_runs");
            update("DELETE FROM feed.submission_row");
            update("DELETE FROM feed.submission");
            update("DELETE FROM feed.freshness");
            update("DELETE FROM feed_ops.pending");
            update("DELETE FROM feed_ops.republish");
            update("DELETE FROM feed_ops.normalization_flags");
            SubmissionTestPurge.purge(jdbc, "s.id LIKE :prefix", Map.of("prefix", ID_PREFIX + "%"));
        });
    }
}
