package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * A submission as stored, either {@code data_submission}'s row or a revision of it in the journal: what
 * {@link SubmissionNormalizer} reads.
 *
 * @param id             the submission's stored id
 * @param formVersionUid the uid of the pinned form version, as stored ({@code template_version_uid})
 * @param formData       the stored document ({@code form_data}); it is read, never changed
 */
public record StoredSubmission(String id, String formVersionUid, JsonNode formData) {

    public StoredSubmission {
        Objects.requireNonNull(id, "id");
    }
}
