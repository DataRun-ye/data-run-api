package org.nmcpye.datarun.jpa.datasubmissionoutbox;

import java.time.Duration;
import java.time.Instant;

/**
 * Usage in your relay:
 * <pre>
 * {@code
 *  var next = BackoffPolicy.defaultPolicy().next(event.getAttempts());
 *  outboxRepo.reschedule(event.getId(), ex.getMessage(), next);
 * }
 * </pre>
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
public final class BackoffPolicy {

    private final Duration base;      // e.g. 2s
    private final Duration max;       // e.g. 1h
    private final double multiplier;  // e.g. 2.0

    public BackoffPolicy(Duration base, Duration max, double multiplier) {
        this.base = base;
        this.max = max;
        this.multiplier = multiplier;
    }

    public Instant next(int attempts) {
        double pow = Math.pow(multiplier, Math.max(0, attempts - 1));
        long millis = (long) Math.min(base.toMillis() * pow, max.toMillis());
        return Instant.now().plusMillis(millis);
    }

    public static BackoffPolicy defaultPolicy() {
        return new BackoffPolicy(Duration.ofSeconds(2), Duration.ofHours(1), 2.0);
    }
}
