package org.nmcpye.datarun.captureshadow.bootstrap;

public record CaptureBootstrapBatchResult(
    long nextSerial,
    int rows,
    long orgUnitAliasesCreated,
    long orgUnitAliasesExisting,
    long identitiesCreated,
    long identitiesExisting,
    long eventsCreated,
    long eventsExisting
) {

    static CaptureBootstrapBatchResult empty(long nextSerial) {
        return new CaptureBootstrapBatchResult(nextSerial, 0, 0, 0, 0, 0, 0, 0);
    }
}
