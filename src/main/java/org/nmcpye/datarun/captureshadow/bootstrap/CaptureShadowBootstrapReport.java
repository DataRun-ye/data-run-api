package org.nmcpye.datarun.captureshadow.bootstrap;

import java.util.List;

public record CaptureShadowBootstrapReport(
    CaptureSourceBoundary sourceBoundary,
    ItemCount orgUnitAliases,
    ItemCount identities,
    ItemCount events,
    ItemCount checkpoints,
    long sourceMovementCount,
    long missingIdentityCount,
    long identityDifferenceCount,
    long extraIdentityCount,
    long missingOrgUnitAliasCount,
    long orgUnitAliasDifferenceCount,
    long missingEventCount,
    long eventDifferenceCount,
    long extraEventCount,
    long checkpointDifferenceCount,
    List<String> diagnosticSamples
) {

    public CaptureShadowBootstrapReport {
        diagnosticSamples = List.copyOf(diagnosticSamples);
    }

    public long differenceCount() {
        return sourceMovementCount
            + missingIdentityCount
            + identityDifferenceCount
            + extraIdentityCount
            + missingOrgUnitAliasCount
            + orgUnitAliasDifferenceCount
            + missingEventCount
            + eventDifferenceCount
            + extraEventCount
            + checkpointDifferenceCount;
    }

    public boolean successful() {
        return differenceCount() == 0;
    }

    public long createdCount() {
        return orgUnitAliases.created()
            + identities.created()
            + events.created()
            + checkpoints.created();
    }

    public String toOperatorText() {
        return "capture-shadow-bootstrap/v1\n"
            + "status=" + (successful() ? "EXACT" : "MISMATCH") + "\n"
            + "source_rows=" + sourceBoundary.sourceCount() + "\n"
            + "source_max_serial=" + value(sourceBoundary.sourceMaxSerial()) + "\n"
            + "source_sha256=" + sourceBoundary.sourceSha256() + "\n"
            + "created=" + createdCount() + "\n"
            + item("org_unit_aliases", orgUnitAliases)
            + item("capture_identities", identities)
            + item("capture_events", events)
            + item("completion_checkpoints", checkpoints)
            + "source_movement=" + sourceMovementCount + "\n"
            + "missing_identities=" + missingIdentityCount + "\n"
            + "identity_differences=" + identityDifferenceCount + "\n"
            + "extra_identities=" + extraIdentityCount + "\n"
            + "missing_org_unit_aliases=" + missingOrgUnitAliasCount + "\n"
            + "org_unit_alias_differences=" + orgUnitAliasDifferenceCount + "\n"
            + "missing_events=" + missingEventCount + "\n"
            + "event_differences=" + eventDifferenceCount + "\n"
            + "extra_events=" + extraEventCount + "\n"
            + "checkpoint_differences=" + checkpointDifferenceCount + "\n"
            + "difference_count=" + differenceCount() + "\n"
            + "diagnostic_samples=" + diagnosticSamples + "\n";
    }

    private static String item(String name, ItemCount count) {
        return name + "_created=" + count.created() + "\n"
            + name + "_existing=" + count.existing() + "\n";
    }

    private static String value(Object value) {
        return value == null ? "null" : value.toString();
    }

    public record ItemCount(long created, long existing) {

        public ItemCount plus(ItemCount other) {
            return new ItemCount(created + other.created, existing + other.existing);
        }
    }
}
