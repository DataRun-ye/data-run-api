package org.nmcpye.datarun.jpa.datasubmission.conflict;

/** Why a phone copy was kept aside as a conflict. */
public enum ConflictCause {
    /** A server-side change came after the copy's base version. */
    SERVER_SIDE_CHANGE,
    /** Detection failed, so the copy was kept aside rather than risk applying it. */
    DETECTION_ERROR
}
