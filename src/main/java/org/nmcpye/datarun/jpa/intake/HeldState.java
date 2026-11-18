package org.nmcpye.datarun.jpa.intake;

/**
 * Where a held upload stands. Discarding is a label, not a deletion: a
 * discarded upload can still be released.
 */
public enum HeldState {
    /** Waiting for an admin's decision. */
    HELD,
    /** Saved into the real data by an admin's release. */
    RELEASED,
    /** Marked as not real by an admin; kept, and can still be released. */
    DISCARDED,
    /** A real submission with the same uid exists, so the held copy was not used. */
    SUPERSEDED
}
