package org.nmcpye.datarun.characterization.a2;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

/**
 * Server-side changes for tests, written the way submission review's methods will write them, before those methods
 * exist: the submission's row, its revision and its outbox row, in one transaction, so the journal stays whole
 * ({@code JournalReconciliation} agrees). Also the decisions a person will make on a conflict.
 */
final class ReviewSeed {

    private final NamedParameterJdbcTemplate jdbc;
    private final PlatformTransactionManager transactionManager;

    ReviewSeed(NamedParameterJdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transactionManager = transactionManager;
    }

    /** A user's id ({@code app_user.id}) by login. */
    String userId(String login) {
        return jdbc.queryForObject("SELECT id FROM app_user WHERE login = :login", Map.of("login", login),
            String.class);
    }

    /** An edit: the top-level name set on the server, by {@code resolverLogin}. */
    void edit(String uid, String resolverLogin, String name) {
        change(uid, """
                UPDATE data_submission
                SET form_data = jsonb_set(form_data, '{main,a2_name}', to_jsonb(CAST(:value AS text))),
                    lock_version = lock_version + 1, last_modified_date = timezone('UTC', now())
                WHERE uid = :uid""",
            new MapSqlParameterSource("value", name), "UPDATE", "EDIT", userId(resolverLogin), null, "UPDATE");
    }

    /** A move to another assignment of the activity: placement and copied labels from the destination. */
    void move(String uid, String resolverLogin, String destination) {
        change(uid, """
                UPDATE data_submission s
                SET assignment_uid = a.uid, team_uid = t.uid, team_code = t.code, org_unit_uid = o.uid,
                    org_unit_code = o.code, org_unit_name = o.name, activity_uid = act.uid,
                    lock_version = s.lock_version + 1, last_modified_date = timezone('UTC', now())
                FROM assignment a
                JOIN team t ON t.id = a.team_id
                JOIN org_unit o ON o.id = a.org_unit_id
                JOIN activity act ON act.id = a.activity_id
                WHERE s.uid = :uid AND a.uid = :destination""",
            new MapSqlParameterSource("destination", destination), "UPDATE", "MOVE", userId(resolverLogin), null,
            "UPDATE");
    }

    /**
     * A re-send that 6.5.6 wrote onto a deleted submission: the uploader's {@code UPLOAD} (or {@code LEGACY_UPLOAD})
     * revision onto the deleted row, which stays deleted, its top-level name set to {@code name}.
     */
    void uploadOntoDeleted(String uid, String uploaderLogin, String command, String name) {
        change(uid, """
                UPDATE data_submission
                SET form_data = jsonb_set(form_data, '{main,a2_name}', to_jsonb(CAST(:value AS text))),
                    lock_version = lock_version + 1, last_modified_date = timezone('UTC', now())
                WHERE uid = :uid AND deleted""",
            new MapSqlParameterSource("value", name), "UPDATE", command, userId(uploaderLogin), null, "DELETE");
    }

    /** A delete through submission review, with a reason. */
    void delete(String uid, String resolverLogin) {
        change(uid, """
                UPDATE data_submission
                SET deleted = true, deleted_at = timezone('UTC', now()), lock_version = lock_version + 1,
                    last_modified_date = timezone('UTC', now())
                WHERE uid = :uid""",
            new MapSqlParameterSource(), "DELETE", "DELETE", userId(resolverLogin), null, "DELETE");
    }

    /**
     * An accept of a phone copy: its document becomes the state, by {@code resolverLogin} on the uploader's behalf,
     * and the conflict records the decision and the version it made.
     *
     * @param reapplied whether the server-side edits were re-applied on top of the copy (then the state is the copy's
     *                  document with the name {@code reappliedName})
     */
    void accept(String conflictId, String resolverLogin, boolean reapplied, String reappliedName) {
        Map<String, Object> conflict = jdbc.queryForMap("""
                SELECT s.uid, c.uploader_user_id, c.phone_copy -> 'formData' AS document
                FROM submission_review.conflict c JOIN data_submission s ON s.id = c.submission_id
                WHERE c.id = :id""",
            Map.of("id", conflictId));
        String uid = (String) conflict.get("uid");
        String resolver = userId(resolverLogin);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            write(uid, """
                    UPDATE data_submission
                    SET form_data = CASE WHEN :reapplied
                                        THEN jsonb_set(CAST(:document AS jsonb), '{main,a2_name}',
                                                       to_jsonb(CAST(:name AS text)))
                                        ELSE CAST(:document AS jsonb) END,
                        lock_version = lock_version + 1, last_modified_date = timezone('UTC', now())
                    WHERE uid = :uid""",
                new MapSqlParameterSource("document", conflict.get("document").toString())
                    .addValue("reapplied", reapplied).addValue("name", reappliedName),
                "UPDATE", "CONFLICT_ACCEPT", resolver, (String) conflict.get("uploader_user_id"), "UPDATE");
            jdbc.update("""
                    UPDATE submission_review.conflict
                    SET state = 'ACCEPTED', decided_at = now(), decided_by_user_id = :resolver,
                        decision_reason = 'the phone was right', reapplied_edits = :reapplied,
                        accepted_version = (SELECT lock_version FROM data_submission WHERE uid = :uid)
                    WHERE id = :id""",
                new MapSqlParameterSource("resolver", resolver).addValue("reapplied", reapplied)
                    .addValue("uid", uid).addValue("id", conflictId));
        });
    }

    /** Keep: the conflict closes with a reason, and the submission stays as it is. */
    void keep(String conflictId, String resolverLogin) {
        String resolver = userId(resolverLogin);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> jdbc.update("""
                UPDATE submission_review.conflict
                SET state = 'KEPT', decided_at = now(), decided_by_user_id = :resolver,
                    decision_reason = 'nothing in the copy to take'
                WHERE id = :id""",
            Map.of("resolver", resolver, "id", conflictId)));
    }

    /**
     * A gap repair's baseline after the release before the journal deleted the submission unrecorded: the row
     * deleted, its version raised, and a {@code BASELINE} by the system, with no outbox row, as the gap repair writes.
     */
    void deletedUnrecorded(String uid) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("""
                    UPDATE data_submission
                    SET deleted = true, deleted_at = timezone('UTC', now()), lock_version = lock_version + 1
                    WHERE uid = :uid""",
                Map.of("uid", uid));
            jdbc.update("""
                    INSERT INTO journal.submission_revision (submission_id, version, previous_version, kind, command,
                                                     actor_system, form_data, metadata)
                    SELECT s.id, s.lock_version, s.lock_version - 1, 'BASELINE', 'GAP_REPAIR', 'MIGRATION',
                           s.form_data, journal.submission_revision_metadata(s)
                    FROM data_submission s WHERE s.uid = :uid""",
                Map.of("uid", uid));
        });
    }

    /**
     * A submission stored before the journal started, made by {@code creatorLogin}: its row, and the baseline that
     * records it, as the journal's first run left every such submission.
     */
    void storedBeforeTheJournal(String uid, String creatorLogin, String assignment, String formVersion,
                                String document) {
        storedBeforeTheJournal(uid, creatorLogin, assignment, formVersion, document, false);
    }

    /** As above; {@code deleted}: deleted before the journal started, so its first revision is a deleted baseline. */
    void storedBeforeTheJournal(String uid, String creatorLogin, String assignment, String formVersion,
                                String document, boolean deleted) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("""
                    INSERT INTO data_submission (id, uid, template_uid, template_version_uid, template_version_no,
                                                 assignment_uid, activity_uid, team_uid, team_code, org_unit_uid,
                                                 org_unit_code, org_unit_name, form_data, lock_version, deleted,
                                                 deleted_at, created_by, created_date, last_modified_by,
                                                 last_modified_date)
                    SELECT :id, :uid, v.template_uid, v.uid, v.version_number, a.uid, act.uid, t.uid, t.code, o.uid,
                           o.code, o.name, CAST(:document AS jsonb), 3, :deleted,
                           CASE WHEN :deleted THEN timezone('UTC', now()) END,
                           :creator, timezone('UTC', now()), :creator, timezone('UTC', now())
                    FROM assignment a
                    JOIN team t ON t.id = a.team_id
                    JOIN org_unit o ON o.id = a.org_unit_id
                    JOIN activity act ON act.id = a.activity_id
                    JOIN data_template_version v ON v.uid = :formVersion
                    WHERE a.uid = :assignment""",
                new MapSqlParameterSource("id", ("01HA2PREJOURNAL" + uid + "00000000000").substring(0, 26))
                    .addValue("uid", uid).addValue("creator", creatorLogin).addValue("assignment", assignment)
                    .addValue("formVersion", formVersion).addValue("document", document)
                    .addValue("deleted", deleted));
            jdbc.update("""
                    INSERT INTO journal.submission_revision (submission_id, version, kind, command, actor_system,
                                                     form_data, metadata)
                    SELECT s.id, s.lock_version, 'BASELINE', 'MIGRATION', 'MIGRATION', s.form_data,
                           journal.submission_revision_metadata(s)
                    FROM data_submission s WHERE s.uid = :uid""",
                Map.of("uid", uid));
        });
    }

    private void change(String uid, String rowChange, MapSqlParameterSource params, String kind, String command,
                        String actor, String onBehalfOf, String eventType) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            write(uid, rowChange, params, kind, command, actor, onBehalfOf, eventType));
    }

    /** The row's change, then its revision and the outbox row that announces it, as the persistence layer writes. */
    private void write(String uid, String rowChange, MapSqlParameterSource params, String kind, String command,
                       String actor, String onBehalfOf, String eventType) {
        if (jdbc.update(rowChange, params.addValue("uid", uid)) != 1) {
            throw new IllegalStateException("No submission " + uid + " to change");
        }
        jdbc.update("""
                INSERT INTO journal.submission_revision (submission_id, version, previous_version, kind, command,
                                                 actor_user_id, on_behalf_of_user_id, reason, form_data, metadata)
                SELECT s.id, s.lock_version, s.lock_version - 1, :kind, :command, :actor, :onBehalfOf, 'a reason',
                       s.form_data, journal.submission_revision_metadata(s)
                FROM data_submission s WHERE s.uid = :uid""",
            new MapSqlParameterSource("uid", uid).addValue("kind", kind).addValue("command", command)
                .addValue("actor", actor).addValue("onBehalfOf", onBehalfOf));
        jdbc.update("""
                INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload,
                                    event_type, status, attempt, submission_version)
                SELECT s.id, s.serial_number, s.uid, s.template_version_uid, s.form_data, :eventType, 'pending', 0,
                       s.lock_version
                FROM data_submission s WHERE s.uid = :uid""",
            Map.of("uid", uid, "eventType", eventType));
    }
}
