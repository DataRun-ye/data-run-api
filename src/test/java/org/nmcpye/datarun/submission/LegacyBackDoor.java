package org.nmcpye.datarun.submission;

import org.nmcpye.datarun.etl.config.EtlProperties;

/** A class of the submission packages that reads the runtime's settings, for {@code LegacyDependencyRuleTest} only. */
@SuppressWarnings("unused")
public class LegacyBackDoor {
    Object rowTimeout(EtlProperties properties) {
        return properties.getRowTimeout();
    }
}
