package org.nmcpye.datarun.jpa.datasubmission.revision;

/**
 * The command a revision records, as {@code submission_revision.command} stores it. These are the application's;
 * Liquibase also writes {@code MIGRATION} (the baseline when the journal started) and {@code GAP_REPAIR} (the state
 * a rolled-back release left unrecorded).
 */
public enum RevisionCommand {
    /** A phone upload on the versioned route. */
    UPLOAD,
    /** An upload on the legacy route (apps before 6.0.2, API callers). */
    LEGACY_UPLOAD,
    /** An admin's release of a held upload, for its uploader. */
    HELD_RELEASE,
    /** An admin's delete. */
    ADMIN_DELETE
}
