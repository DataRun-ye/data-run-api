package org.nmcpye.datarun.captureshadow.bootstrap;

import java.util.List;

record CaptureComparisonResult(
    long missingIdentityCount,
    long identityDifferenceCount,
    long extraIdentityCount,
    long missingOrgUnitAliasCount,
    long orgUnitAliasDifferenceCount,
    long missingEventCount,
    long eventDifferenceCount,
    long extraEventCount,
    long missingCurrentPointerCount,
    long currentPointerDifferenceCount,
    long extraCurrentPointerCount,
    List<String> diagnosticSamples
) {

    long foundationDifferenceCount() {
        return missingIdentityCount
            + identityDifferenceCount
            + extraIdentityCount
            + missingOrgUnitAliasCount
            + orgUnitAliasDifferenceCount
            + missingEventCount
            + eventDifferenceCount
            + extraEventCount;
    }

    long differenceCount() {
        return foundationDifferenceCount()
            + missingCurrentPointerCount
            + currentPointerDifferenceCount
            + extraCurrentPointerCount;
    }
}
