package org.nmcpye.datarun.submission.normalized;

import java.util.Objects;

/**
 * A submission as stored, either {@code data_submission}'s row or a revision of it in the journal: what
 * {@link SubmissionNormalizer} reads.
 *
 * @param id             the submission's stored id
 * @param formUid        the uid of the submission's form, as stored ({@code data_submission.template_uid}, which
 *                       no change alters, so the journal's revisions leave it out): the form whose definition a
 *                       caller reads ({@code FormDefinitionJdbcReader}) and normalizes against; may be {@code null}
 *                       when unknown, and then the caller has no definition to pass
 * @param formVersionUid the uid of the pinned form version, as stored ({@code template_version_uid}); may be
 *                       {@code null} or unknown, which the normalizer flags
 * @param formData       the stored document as JSON text ({@code form_data::text}), or {@code null} when there is
 *                       none; the text {@code null} means the same (both flagged
 *                       {@link NormalizationFlag#DOCUMENT_MISSING}). The normalizer parses it itself,
 *                       exactly ({@link StoredJson}): a document read through another mapper may already have lost
 *                       digits
 */
public record StoredSubmission(String id, String formUid, String formVersionUid, String formData) {

    public StoredSubmission {
        Objects.requireNonNull(id, "id");
    }
}
