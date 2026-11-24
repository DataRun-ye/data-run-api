package org.nmcpye.datarun.jpa.datasubmission.revision;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;

/**
 * Appends revisions to the submission change record ({@code submission_revision}). Only the submission persistence
 * layer calls it, after it has flushed a change, in the transaction of that change: the revision commits or rolls
 * back with the row it records.
 *
 * <p>The state is copied from the row as stored, by the database: {@code form_data} as it is, and the other columns
 * through {@code submission_revision_metadata()}, the definition the baseline and the reconciliation check use too.
 * Nothing the application holds in memory can differ from what the revision records.</p>
 */
@Repository
@RequiredArgsConstructor
public class SubmissionRevisionWriter {

    private static final String APPEND_SQL = """
        INSERT INTO submission_revision (submission_id, version, previous_version, kind, command, actor_user_id,
                                         on_behalf_of_user_id, reason, form_data, metadata)
        SELECT s.id, s.lock_version, :previousVersion, :kind, :command, :actorUserId, :onBehalfOf, :reason,
               s.form_data, submission_revision_metadata(s)
        FROM data_submission s
        WHERE s.id = :submissionId AND s.lock_version = :version
        """;

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Records that the submission reached {@code version} by a change of the given kind.
     *
     * @param submissionId    the submission's id ({@code data_submission.id})
     * @param version         its version after the change, which the stored row must have
     * @param previousVersion its version before the change; null when it was created
     * @throws IllegalStateException when the stored row is not at {@code version}: the change was not flushed, or
     *                               something else wrote the row
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String submissionId, long version, Long previousVersion, RevisionKind kind,
                       RevisionContext context) {
        int appended = jdbc.update(APPEND_SQL, new MapSqlParameterSource()
            .addValue("submissionId", submissionId)
            .addValue("version", version)
            .addValue("previousVersion", previousVersion, Types.BIGINT)
            .addValue("kind", kind.name())
            .addValue("command", context.command().name())
            .addValue("actorUserId", context.actorUserId())
            .addValue("onBehalfOf", context.onBehalfOfUserId(), Types.VARCHAR)
            .addValue("reason", context.reason(), Types.VARCHAR));
        if (appended != 1) {
            throw new IllegalStateException(
                "Submission " + submissionId + " is not stored at version " + version + " after its change");
        }
    }
}
