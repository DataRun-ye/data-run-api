package org.nmcpye.datarun.jpa.datasubmission.conflict;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.nmcpye.datarun.jpa.datasubmission.revision.SubmissionRevisionReader;
import org.nmcpye.datarun.submission.normalized.StoredJson;

/**
 * What the conflict comparison reads of a submission's state or of a phone copy (spec, Conflicts). A copy is unchanged
 * only if applying it would write nothing the client owns differently, so every field an upload writes from the copy
 * onto a stored submission ({@code DefaultDataSubmissionService.updateMutableFields}, and its delete) is compared,
 * except the labels that follow the assignment:
 * <ul>
 *   <li>compared: the document ({@code form_data}, rows' structure keys left out), the deleted flag (a deleted copy
 *       deletes a live submission; a live copy against a deleted state counts as a change too), the assignment, and
 *       {@code status}, which phones never send and API callers can;</li>
 *   <li>not compared: {@code activity_uid}, {@code team_uid}, {@code team_code}, {@code org_unit_uid},
 *       {@code org_unit_code} and {@code org_unit_name}, which both routes set from the assignment before the save, so
 *       they follow it;</li>
 *   <li>not compared: the form version. An update never writes it: {@code template_version_uid} is set at creation
 *       and every later upload keeps it (existing behaviour, golden W1-07), so a copy that differs only in its form
 *       version, with identical data, changes nothing.</li>
 * </ul>
 * Nothing else an upload sends is written onto a stored submission: entry times and the creation's own fields are set
 * at creation, and {@code deleted_at} is the server's time.
 *
 * @param document   the document ({@code formData}), or null when there is none
 * @param assignment the assignment's uid
 * @param status     the progress status ({@code data_submission.status}), or null when there is none
 */
public record ComparedState(JsonNode document, boolean deleted, String assignment, String status) {

    /** A stored state, its document read exactly from the text the database printed ({@link StoredJson}). */
    static ComparedState of(SubmissionRevisionReader.State stored) {
        return new ComparedState(parse(stored.document()), stored.deleted(), stored.assignment(), stored.status());
    }

    /**
     * The parts of an upload item as the phone sent them, in either route's shape, which name them alike:
     * {@code formData}, {@code deleted} (absent or null is false), {@code assignment} and {@code status}.
     */
    static ComparedState ofUploadItem(JsonNode item) {
        JsonNode deleted = item.path("deleted");
        return new ComparedState(
            item.get("formData"),
            deleted.isBoolean() && deleted.booleanValue(),
            textOrNull(item.get("assignment")),
            textOrNull(item.get("status")));
    }

    /** Stored text parsed with every number exact; null text is no document. */
    static JsonNode parse(String text) {
        if (text == null) {
            return null;
        }
        try {
            return StoredJson.parse(text);
        } catch (JsonProcessingException e) {
            // StoredJson's message names only the kind of failure, never the text
            throw new IllegalStateException("A stored document is not readable JSON: " + e.getOriginalMessage());
        }
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}
