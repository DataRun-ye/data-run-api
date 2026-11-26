package org.nmcpye.datarun.submission.normalized;

import java.util.Objects;

/**
 * A submission as stored, either {@code data_submission}'s row or a revision of it in the journal: what
 * {@link SubmissionNormalizer} reads.
 *
 * @param id            the submission's stored id
 * @param formId        the id of the submission's form: the form whose definition a caller reads
 *                      ({@code FormDefinitionJdbcReader}) and normalizes against. The submission stores its form by
 *                      uid ({@code data_submission.template_uid}, which no change alters, so the journal's revisions
 *                      leave it out); the reader turns it into this id. {@code null} when the uid names no stored
 *                      form, and then the caller has no definition to pass
 * @param formVersionId the id of the pinned form version, which the submission stores by uid
 *                      ({@code template_version_uid}) and the reader turns into this id; {@code null} when it names
 *                      none of the form's versions, or none is stored, which the normalizer flags
 * @param formData       the stored document as JSON text ({@code form_data::text}), or {@code null} when there is
 *                       none; the text {@code null} means the same (both flagged
 *                       {@link NormalizationFlag#DOCUMENT_MISSING}). The normalizer parses it itself,
 *                       exactly ({@link StoredJson}): a document read through another mapper may already have lost
 *                       digits
 */
public record StoredSubmission(String id, String formId, String formVersionId, String formData) {

    public StoredSubmission {
        Objects.requireNonNull(id, "id");
    }
}
