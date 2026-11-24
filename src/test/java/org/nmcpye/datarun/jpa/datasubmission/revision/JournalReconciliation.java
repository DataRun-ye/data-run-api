package org.nmcpye.datarun.jpa.datasubmission.revision;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What the integrity check's journal lines (check 11) count in production, for chosen submissions in a test: every
 * submission has a head revision at its version whose state is its row, each revision written by the application
 * has exactly one outbox row, that row's type follows the revision's state, and no outbox row of theirs lacks a
 * version.
 */
public final class JournalReconciliation {

    private JournalReconciliation() {
    }

    /** A finding per disagreement, naming the submission by uid; empty when the journal agrees with the rows. */
    public static List<String> findings(NamedParameterJdbcTemplate jdbc, String where, Map<String, ?> params) {
        List<String> findings = new ArrayList<>();
        jdbc.query("""
            SELECT s.uid, h.version AS head, s.lock_version,
                   h.form_data IS NOT DISTINCT FROM s.form_data
                       AND h.metadata = submission_revision_metadata(s) AS same_state,
                   (SELECT count(*) FROM submission_revision r
                    WHERE r.submission_id = s.id AND r.kind <> 'BASELINE'
                      AND (SELECT count(*) FROM outbox o
                           WHERE o.submission_id = r.submission_id AND o.submission_version = r.version) <> 1)
                       AS unannounced,
                   (SELECT count(*) FROM outbox o
                    JOIN submission_revision r ON r.submission_id = o.submission_id AND r.version = o.submission_version
                    WHERE o.submission_id = s.id
                      AND (o.event_type = 'DELETE') IS DISTINCT FROM (r.metadata ->> 'deleted')::boolean) AS mistyped,
                   (SELECT count(*) FROM outbox o WHERE o.submission_id = s.id AND o.submission_version IS NULL)
                       AS unversioned
            FROM data_submission s
            LEFT JOIN LATERAL (SELECT r.version, r.form_data, r.metadata FROM submission_revision r
                               WHERE r.submission_id = s.id ORDER BY r.version DESC LIMIT 1) h ON true
            WHERE %s
            ORDER BY s.serial_number""".formatted(where), params, rs -> {
            String uid = rs.getString("uid");
            long head = rs.getLong("head");
            if (rs.wasNull()) {
                findings.add(uid + ": no revision");
                return;
            }
            if (head != rs.getLong("lock_version")) {
                findings.add(uid + ": head revision at " + head + ", row at " + rs.getLong("lock_version"));
            } else if (!rs.getBoolean("same_state")) {
                findings.add(uid + ": the head revision's state is not the row's");
            }
            if (rs.getLong("unannounced") > 0) {
                findings.add(uid + ": " + rs.getLong("unannounced") + " revisions without exactly one outbox row");
            }
            if (rs.getLong("mistyped") > 0) {
                findings.add(uid + ": " + rs.getLong("mistyped") + " outbox rows whose type does not follow the state");
            }
            if (rs.getLong("unversioned") > 0) {
                findings.add(uid + ": " + rs.getLong("unversioned") + " outbox rows without a version");
            }
        });
        return findings;
    }
}
