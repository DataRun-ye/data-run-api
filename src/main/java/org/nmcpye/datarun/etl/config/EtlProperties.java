package org.nmcpye.datarun.etl.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings of the outbox consumer that feeds {@code analytics.tall_canonical} and {@code analytics.events}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "datarun.etl")
public class EtlProperties {

    /**
     * How long a claimed row stays reserved for the run that claimed it. A run that dies mid-batch leaves its
     * rows to be claimed again once this has passed; it must exceed the time one batch takes.
     */
    private Duration claimLease = Duration.ofMinutes(15);
}
