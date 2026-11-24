package org.nmcpye.datarun.jpa.datasubmission.revision;

/**
 * What a revision did to its submission, as {@code submission_revision.kind} stores it. These are the application's;
 * Liquibase writes {@code BASELINE}, the state of a submission when the journal started or after a gap.
 */
public enum RevisionKind {
    /** The submission was stored: version 0. A new submission sent already deleted is created deleted. */
    CREATE,
    /** A stored submission changed. It may stay deleted: a re-send onto a deleted submission changes its data. */
    UPDATE,
    /** A live submission became deleted. */
    DELETE
}
