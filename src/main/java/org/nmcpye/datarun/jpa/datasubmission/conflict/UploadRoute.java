package org.nmcpye.datarun.jpa.datasubmission.conflict;

/** The upload route a phone copy came by, which gives its upload item's shape. */
public enum UploadRoute {
    /** {@code POST /api/v1/dataSubmission/bulk?referenceVersion=1}: apps 6.0.2 and later. */
    VERSIONED,
    /** {@code POST /api/v1/dataSubmission/bulk}: apps before 6.0.2, and API callers. */
    LEGACY
}
