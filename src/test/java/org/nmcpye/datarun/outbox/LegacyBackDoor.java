package org.nmcpye.datarun.outbox;

import org.nmcpye.datarun.etl.config.EtlProperties;

/** An outbox class that reads the runtime's settings, for {@code LegacyDependencyRuleTest} only. */
@SuppressWarnings("unused")
public class LegacyBackDoor {
    Object batchSize(EtlProperties properties) {
        return properties.getBatchSize();
    }
}
