package org.nmcpye.datarun.config.datarun;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "outbox.relay")
public class OutboxProperties {
    /**
     * How often the relay polls (ms).
     */
    @Min(100)
    private long pollIntervalMs = 20000;

    /**
     * How many events to claim per poll.
     */
    @Min(1)
    private int batchSize = 50;

    /**
     * How many worker threads to use for concurrent processing of claimed events.
     */
    @Min(1)
    private int workerThreads = 4;

    /**
     * After how many attempts the event is moved to FAILED (DLQ).
     */
    @Min(1)
    private int maxAttempts = 10;
}
