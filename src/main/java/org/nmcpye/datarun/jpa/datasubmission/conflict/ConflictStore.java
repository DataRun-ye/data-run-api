package org.nmcpye.datarun.jpa.datasubmission.conflict;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Conflicts, in {@code submission_review.conflict}. Detection writes them in the upload item's transaction, under the
 * submission lock, so one submission's conflicts never change concurrently. Times are the database's transaction time.
 */
@Repository
@RequiredArgsConstructor
public class ConflictStore {

    /** An open conflict: its id and its phone copy as stored text. */
    public record OpenConflict(String id, String phoneCopy) {
    }

    /** A conflict to open. A detection error may not know the base version. */
    public record NewConflict(String submissionId, String uploaderUserId, ConflictCause cause, Long baseVersion,
                              boolean rowsLackIds, UploadRoute route, String phoneCopy) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    /** The uploader's open conflict on the submission, if any: there is at most one. */
    public Optional<OpenConflict> findOpen(String submissionId, String uploaderUserId) {
        return jdbc.query("""
                SELECT id, phone_copy::text AS phone_copy FROM submission_review.conflict
                WHERE submission_id = :submissionId AND uploader_user_id = :uploader AND state = 'OPEN'""",
            Map.of("submissionId", submissionId, "uploader", uploaderUserId),
            (row, n) -> new OpenConflict(row.getString("id"), row.getString("phone_copy"))).stream().findFirst();
    }

    /** The phone copy, as stored text, of the uploader's conflict on the submission that was decided last. */
    public Optional<String> lastDecidedCopy(String submissionId, String uploaderUserId) {
        return jdbc.queryForList("""
                SELECT phone_copy::text FROM submission_review.conflict
                WHERE submission_id = :submissionId AND uploader_user_id = :uploader AND state <> 'OPEN'
                ORDER BY decided_at DESC, id DESC
                LIMIT 1""",
            Map.of("submissionId", submissionId, "uploader", uploaderUserId), String.class).stream().findFirst();
    }

    /**
     * The submission versions that plain accepts of the uploader's copies made: an accept that re-applied no
     * server-side edit leaves the state equal to the copy, so its revision is a base version of theirs.
     */
    public Set<Long> plainlyAcceptedVersions(String submissionId, String uploaderUserId) {
        return new HashSet<>(jdbc.queryForList("""
                SELECT accepted_version FROM submission_review.conflict
                WHERE submission_id = :submissionId AND uploader_user_id = :uploader AND state = 'ACCEPTED'
                  AND NOT reapplied_edits""",
            Map.of("submissionId", submissionId, "uploader", uploaderUserId), Long.class));
    }

    /** Opens a conflict, received now; returns its id. */
    public String open(NewConflict conflict) {
        String id = CodeGenerator.nextUlid();
        jdbc.update("""
                INSERT INTO submission_review.conflict (id, submission_id, uploader_user_id, state, cause, base_version,
                                                        rows_lack_ids, upload_route, phone_copy, first_received_at,
                                                        last_received_at)
                VALUES (:id, :submissionId, :uploader, 'OPEN', :cause, :base, :rowsLackIds, :route,
                        CAST(:copy AS jsonb), now(), now())""",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("submissionId", conflict.submissionId())
                .addValue("uploader", conflict.uploaderUserId())
                .addValue("cause", conflict.cause().name())
                .addValue("base", conflict.baseVersion(), Types.BIGINT)
                .addValue("rowsLackIds", conflict.rowsLackIds())
                .addValue("route", conflict.route().name())
                .addValue("copy", conflict.phoneCopy()));
        return id;
    }

    /** The open conflict's copy arrived again, the same: one more re-send, received now. */
    public void sentAgain(String id) {
        jdbc.update("""
                UPDATE submission_review.conflict SET resend_count = resend_count + 1, last_received_at = now()
                WHERE id = :id AND state = 'OPEN'""",
            Map.of("id", id));
    }

    /**
     * A different copy arrived while the conflict is open: it replaces the copy kept, with what detection found of it,
     * keeps the first receipt time, and raises the conflict's own version, so a decision made on the older copy can be
     * refused. A base version detection could not find leaves the known one, and so does a null cause: only the open
     * conflict keeps the new copy aside.
     */
    public void replace(String id, ConflictCause cause, Long baseVersion, boolean rowsLackIds, UploadRoute route,
                        String phoneCopy) {
        jdbc.update("""
                UPDATE submission_review.conflict
                SET cause = coalesce(:cause, cause), base_version = coalesce(:base, base_version),
                    rows_lack_ids = :rowsLackIds,
                    upload_route = :route, phone_copy = CAST(:copy AS jsonb), resend_count = resend_count + 1,
                    last_received_at = now(), version = version + 1
                WHERE id = :id AND state = 'OPEN'""",
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("cause", cause == null ? null : cause.name(), Types.VARCHAR)
                .addValue("base", baseVersion, Types.BIGINT)
                .addValue("rowsLackIds", rowsLackIds)
                .addValue("route", route.name())
                .addValue("copy", phoneCopy));
    }
}
