package org.nmcpye.datarun.jpa.datasubmission.conflict;

/**
 * A stored submission as conflict detection reads it: plain values the upload gives from the submission it holds under
 * the submission lock, never the entity itself.
 *
 * @param submissionId      its id ({@code data_submission.id}), by which the journal and the conflicts name it
 * @param version           its version, the head revision's
 * @param documentText      its document as the database prints it ({@code form_data::text}), every number exact, or
 *                          null when it has none
 * @param deleted           whether it is deleted
 * @param assignment        its assignment's uid
 * @param status            its progress status, or null when it has none
 * @param uploaderIsCreator whether the uploader made it, as the upload's stored-record guard tells: a creator's base
 *                          version can be a baseline of the live submission
 */
public record StoredState(String submissionId, long version, String documentText, boolean deleted, String assignment,
                          String status, boolean uploaderIsCreator) {

    /** What the conflict comparison reads of it, its document parsed exactly. */
    ComparedState compared() {
        return new ComparedState(ComparedState.parse(documentText), deleted, assignment, status);
    }
}
