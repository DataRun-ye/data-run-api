package org.nmcpye.datarun.transition;

import java.time.Instant;

public final class TransitionTimestamp {

    private static final long NANOS_PER_MICROSECOND = 1_000L;
    private static final long MICROSECONDS_PER_SECOND = 1_000_000L;

    private TransitionTimestamp() {
    }

    public static Instant toDatabasePrecision(Instant value) {
        long roundedMicros = (
            value.getNano() + NANOS_PER_MICROSECOND / 2
        ) / NANOS_PER_MICROSECOND;
        if (roundedMicros == MICROSECONDS_PER_SECOND) {
            return Instant.ofEpochSecond(value.getEpochSecond() + 1);
        }
        return Instant.ofEpochSecond(
            value.getEpochSecond(),
            roundedMicros * NANOS_PER_MICROSECOND
        );
    }
}
