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

    /** Whether this instance polls the outbox at all. */
    private boolean enabled = true;

    /** The pause between the end of one poll and the start of the next. */
    private Duration pollDelay = Duration.ofSeconds(30);

    /** Rows claimed in one batch; each batch is one etl_runs row. */
    private int batchSize = 100;

    /**
     * How long one poll keeps claiming batches while full batches come back. It bounds how long the ETL works
     * without a pause, so it shares the database with uploads; with {@link #pollDelay} it caps its duty cycle.
     */
    private Duration maxPollDuration = Duration.ofSeconds(60);

    /**
     * How long a claimed row stays reserved for the run that claimed it. A run that dies mid-batch leaves its
     * rows to be claimed again once this has passed; it must exceed the time one batch takes.
     */
    private Duration claimLease = Duration.ofMinutes(15);

    /** Tries before a failing row is dead-lettered (status {@code dead}) and left for an operator. */
    private int maxAttempts = 10;

    /** Wait before the first retry of a failed row; it doubles with each further failure. */
    private Duration retryDelay = Duration.ofMinutes(1);

    /** The longest wait between two tries. */
    private Duration maxRetryDelay = Duration.ofHours(1);

    /** How long the backlog gauges reuse one reading of the outbox, however often they are scraped. */
    private Duration backlogRefresh = Duration.ofSeconds(15);
}
