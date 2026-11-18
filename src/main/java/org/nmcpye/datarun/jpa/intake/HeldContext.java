package org.nmcpye.datarun.jpa.intake;

import java.time.Instant;

/**
 * The server-checked context of a held upload, kept beside its payload so
 * admins can find and judge held uploads without opening them.
 */
public record HeldContext(
    String activity,
    String orgUnit,
    String orgUnitPath,
    String form,
    String formVersion,
    String team,
    String assignment,
    String submittedBy,
    Instant startedAt,
    Instant receivedAt) {
}
