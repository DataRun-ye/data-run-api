package org.nmcpye.datarun.jpa.datasubmission.revision;

/**
 * The command a revision records, as {@code submission_revision.command} stores it. These are the application's;
 * Liquibase also writes {@code MIGRATION} (the baseline when the journal started) and {@code GAP_REPAIR} (the state
 * a rolled-back release left unrecorded).
 *
 * <p>Submission review adds one command per changing method, from {@link #EDIT} on. Conflict detection reads them:
 * each is a server-side change, except an accept of the uploader's own copy, which is their base version. Nothing
 * writes them until those methods are built.</p>
 */
public enum RevisionCommand {
    /** A phone upload on the versioned route. */
    UPLOAD,
    /** An upload on the legacy route (apps before 6.0.2, API callers). */
    LEGACY_UPLOAD,
    /** An admin's release of a held upload, for its uploader. */
    HELD_RELEASE,
    /** An admin's delete. */
    ADMIN_DELETE,
    /** An edit: an ordered list of edit operations, with a reason. */
    EDIT,
    /** A move of the whole submission to another assignment of its activity. */
    MOVE,
    /** A split: rows taken into a new submission (both submissions' revisions carry it). */
    SPLIT,
    /** A delete through submission review, with a reason. */
    DELETE,
    /** A restore of a deleted submission. */
    RESTORE,
    /** An accepted conflict: the uploader's phone copy made the state, by the resolver on the uploader's behalf. */
    CONFLICT_ACCEPT
}
