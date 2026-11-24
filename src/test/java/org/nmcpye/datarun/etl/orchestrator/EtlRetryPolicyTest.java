package org.nmcpye.datarun.etl.orchestrator;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.config.EtlProperties;

import java.time.Duration;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class EtlRetryPolicyTest {

    private final EtlRetryPolicy defaults = new EtlRetryPolicy(new EtlProperties());

    @Test
    void theWaitDoublesFromOneMinuteUpToAnHour() {
        assertThat(IntStream.rangeClosed(1, 9).mapToObj(defaults::delayAfter).map(Duration::toMinutes))
            .containsExactly(1L, 2L, 4L, 8L, 16L, 32L, 60L, 60L, 60L);
    }

    @Test
    void theTenthAttemptIsTheLastAndAnEleventhClaimIsTooMany() {
        assertThat(defaults.isLastAttempt(9)).isFalse();
        assertThat(defaults.isLastAttempt(10)).isTrue();
        assertThat(defaults.wasClaimedTooOften(10)).isFalse();
        assertThat(defaults.wasClaimedTooOften(11)).isTrue();
    }

    @Test
    void aVeryLateAttemptStillWaitsTheCapWithoutOverflowing() {
        assertThat(defaults.delayAfter(1_000)).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void settingsChangeTheScheduleAndAtLeastOneTryIsAlwaysMade() {
        EtlProperties properties = new EtlProperties();
        properties.setRetryDelay(Duration.ofSeconds(10));
        properties.setMaxRetryDelay(Duration.ofSeconds(25));
        properties.setMaxAttempts(0);
        EtlRetryPolicy policy = new EtlRetryPolicy(properties);

        assertThat(policy.delayAfter(1)).isEqualTo(Duration.ofSeconds(10));
        assertThat(policy.delayAfter(2)).isEqualTo(Duration.ofSeconds(20));
        assertThat(policy.delayAfter(3)).isEqualTo(Duration.ofSeconds(25));
        assertThat(policy.isLastAttempt(1)).isTrue();
    }
}
