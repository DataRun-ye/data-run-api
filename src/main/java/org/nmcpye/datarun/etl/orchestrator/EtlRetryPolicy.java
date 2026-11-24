package org.nmcpye.datarun.etl.orchestrator;

import org.nmcpye.datarun.etl.config.EtlProperties;

import java.time.Duration;

/**
 * When a failed outbox row is tried again, and when it is given up as dead. Attempts count claims: a row on
 * attempt {@code n} has been claimed {@code n} times.
 */
final class EtlRetryPolicy {

    private final int maxAttempts;
    private final Duration retryDelay;
    private final Duration maxRetryDelay;

    EtlRetryPolicy(EtlProperties properties) {
        this.maxAttempts = Math.max(1, properties.getMaxAttempts());
        this.retryDelay = properties.getRetryDelay();
        this.maxRetryDelay = properties.getMaxRetryDelay();
    }

    /** Whether a row that failed on this attempt has no try left. */
    boolean isLastAttempt(int attempt) {
        return attempt >= maxAttempts;
    }

    /**
     * Whether a row was claimed more often than it may be tried. A row only gets there when runs ended on it
     * without recording any outcome, for instance a process that died while projecting it.
     */
    boolean wasClaimedTooOften(int attempt) {
        return attempt > maxAttempts;
    }

    /** The wait after failing on this attempt: the retry delay, doubled for each earlier attempt, at most the cap. */
    Duration delayAfter(int attempt) {
        int doublings = Math.min(Math.max(attempt - 1, 0), 20);
        Duration delay = retryDelay.multipliedBy(1L << doublings);
        return delay.compareTo(maxRetryDelay) > 0 ? maxRetryDelay : delay;
    }
}
