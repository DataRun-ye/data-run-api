package org.nmcpye.datarun.jpa.datasubmission.conflict;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.nmcpye.datarun.jpa.datasubmission.revision.SubmissionRevisionReader;
import org.nmcpye.datarun.submission.normalized.StoredJson;

/**
 * What the conflict comparison reads of a submission's state or of a phone copy: the document, the deleted flag, the
 * assignment and the form version (spec, Conflicts).
 *
 * @param document    the document ({@code formData}), or null when there is none
 * @param assignment  the assignment's uid
 * @param formVersion the form version's uid
 */
public record ComparedState(JsonNode document, boolean deleted, String assignment, String formVersion) {

    /** A stored state, its document read exactly from the text the database printed ({@link StoredJson}). */
    static ComparedState of(SubmissionRevisionReader.State stored) {
        return new ComparedState(parse(stored.document()), stored.deleted(), stored.assignment(),
            stored.formVersion());
    }

    /**
     * The parts of an upload item as the phone sent them, in either route's shape, which name them alike:
     * {@code formData}, {@code deleted} (absent or null is false), {@code assignment} and {@code formVersion}.
     */
    static ComparedState ofUploadItem(JsonNode item) {
        JsonNode deleted = item.path("deleted");
        return new ComparedState(
            item.get("formData"),
            deleted.isBoolean() && deleted.booleanValue(),
            textOrNull(item.get("assignment")),
            textOrNull(item.get("formVersion")));
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
