package org.nmcpye.datarun.transition;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TransitionTimestampTest {

    @Test
    void roundsToPostgresMicrosecondPrecision() {
        assertThat(TransitionTimestamp.toDatabasePrecision(
            Instant.parse("2026-07-29T01:34:18.604289627Z")
        )).isEqualTo(Instant.parse("2026-07-29T01:34:18.604290Z"));
        assertThat(TransitionTimestamp.toDatabasePrecision(
            Instant.parse("2026-07-29T01:34:18.999999627Z")
        )).isEqualTo(Instant.parse("2026-07-29T01:34:19Z"));
    }
}
