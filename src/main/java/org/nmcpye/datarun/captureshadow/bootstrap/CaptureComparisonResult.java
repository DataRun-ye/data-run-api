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
    List<String> diagnosticSamples
) {

    long differenceCount() {
        return missingIdentityCount
            + identityDifferenceCount
            + extraIdentityCount
            + missingOrgUnitAliasCount
            + orgUnitAliasDifferenceCount
            + missingEventCount
            + eventDifferenceCount
            + extraEventCount;
    }
}
