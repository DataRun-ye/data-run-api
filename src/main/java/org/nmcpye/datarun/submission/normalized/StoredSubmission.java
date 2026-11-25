package org.nmcpye.datarun.submission.normalized;

import java.util.Objects;

/**
 * A submission as stored, either {@code data_submission}'s row or a revision of it in the journal: what
 * {@link SubmissionNormalizer} reads.
 *
 * @param id             the submission's stored id
 * @param formVersionUid the uid of the pinned form version, as stored ({@code template_version_uid}); may be
 *                       {@code null} or unknown, which the normalizer flags
 * @param formData       the stored document as JSON text ({@code form_data::text}), or {@code null} when there is
 *                       none. The normalizer parses it itself, exactly ({@link StoredJson}): a document read through
 *                       another mapper may already have lost digits
 */
public record StoredSubmission(String id, String formVersionUid, String formData) {

    public StoredSubmission {
        Objects.requireNonNull(id, "id");
    }
}
