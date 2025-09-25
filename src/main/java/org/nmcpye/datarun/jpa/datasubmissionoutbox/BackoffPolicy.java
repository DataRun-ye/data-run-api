package org.nmcpye.datarun.jpa.datasubmissionoutbox;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Usage in your relay:
 * <pre>
 * {@code
 *  var next = BackoffPolicy.defaultPolicy().next(event.getAttempts());
 *  outboxRepo.reschedule(event.getId(), ex.getMessage(), next);
 * }
 * </pre>
 *
 * @author Hamza Assada
 * @since 15/08/2025
 */
public final class BackoffPolicy {

    private final int baseSeconds;
    private final int jitterSeconds;
    private final int maxSeconds;


    public BackoffPolicy(int baseSeconds, int jitterSeconds, int maxSeconds) {
        this.baseSeconds = baseSeconds;
        this.jitterSeconds = jitterSeconds;
        this.maxSeconds = maxSeconds;
    }

    public static BackoffPolicy defaultPolicy() {
        return new BackoffPolicy(30, 5, 3600); // base=30s, jitter up to 5s, max=1h
    }

    /**
     * Compute next available time for a given attempt number.
     * attempts: 1 => baseSeconds, 2 => base*2, etc (attempts is the current attempt count, incremented on claim)
     */
    public Instant next(int attempts) {
        if (attempts < 1) attempts = 1;
        // exponential backoff
        long delay = (long) baseSeconds * (1L << (Math.max(0, attempts - 1)));
        if (delay > maxSeconds) delay = maxSeconds;
        // jitter
        int jitter = ThreadLocalRandom.current().nextInt(0, jitterSeconds + 1);
        long total = Math.min(delay + jitter, maxSeconds);
        return Instant.now().plus(Duration.ofSeconds(total));
    }
}
