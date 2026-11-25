package org.nmcpye.datarun.jpa.datasubmission.revision;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

/**
 * Removes test submissions by SQL, with their revisions and outbox rows: what fixtures do between tests.
 *
 * <p>The journal refuses deletes, and its foreign key keeps a submission that has revisions. The purge sets
 * {@code session_replication_role = replica} for its statements, which disables both (and every other trigger)
 * and which only a superuser may set: the test container's user. No main code sets it; the SQL tripwire in
 * {@code SubmissionWriteOwnershipTest} checks that.</p>
 */
public final class SubmissionTestPurge {

    private SubmissionTestPurge() {
    }

    /**
     * Deletes the submissions matching {@code where}, a condition on {@code data_submission s}, with their revisions,
     * outbox rows and conflicts. Runs in the caller's transaction, which it requires.
     */
    public static void purge(NamedParameterJdbcTemplate jdbc, String where, Map<String, ?> params) {
        jdbc.getJdbcTemplate().execute("SET LOCAL session_replication_role = replica");
        jdbc.update("DELETE FROM submission_review.conflict c USING data_submission s WHERE c.submission_id = s.id AND ("
            + where + ")", params);
        jdbc.update("DELETE FROM outbox o USING data_submission s WHERE o.submission_id = s.id AND (" + where + ")",
            params);
        jdbc.update("""
            DELETE FROM journal.submission_revision r USING data_submission s
            WHERE r.submission_id = s.id AND (%s)""".formatted(where), params);
        jdbc.update("DELETE FROM data_submission s WHERE " + where, params);
        jdbc.getJdbcTemplate().execute("SET LOCAL session_replication_role = DEFAULT");
    }
}
