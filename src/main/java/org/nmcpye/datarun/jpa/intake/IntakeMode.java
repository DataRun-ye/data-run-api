package org.nmcpye.datarun.jpa.intake;

/** How an upload reaches the server. */
public enum IntakeMode {
    /** The normal upload route: go-live rules apply. */
    LIVE,
    /** The practice upload route: always held. */
    PRACTICE,
    /** An admin releasing a held upload: always saved. */
    RELEASE
}
