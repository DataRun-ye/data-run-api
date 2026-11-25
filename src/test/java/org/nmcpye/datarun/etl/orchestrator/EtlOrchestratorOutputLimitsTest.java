package org.nmcpye.datarun.etl.orchestrator;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.outbox.dto.OutboxDto;
import org.nmcpye.datarun.outbox.processing.ChangeOutput;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The runtime refuses to start while an output that is on keeps a time limit that does not stay below its row's. */
class EtlOrchestratorOutputLimitsTest {

    private static final Duration ROW_TIMEOUT = Duration.ofSeconds(60);

    private static ChangeOutput output(String name, boolean on, Duration limit) {
        return new ChangeOutput() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean isOn() {
                return on;
            }

            @Override
            public Optional<Duration> timeLimit() {
                return Optional.ofNullable(limit);
            }

            @Override
            public void apply(OutboxDto change, long deadlineNanos) {
                // not applied here
            }
        };
    }

    @Test
    void limitsBelowTheRowTimeoutAndOutputsWithoutALimitStart() {
        assertThatCode(() -> EtlOrchestrator.requireLimitsBelowTheRowTimeout(List.of(
            output("limited", true, Duration.ofSeconds(20)),
            output("unlimited", true, null)), ROW_TIMEOUT)).doesNotThrowAnyException();
    }

    @Test
    void aLimitAtOrAboveTheRowTimeoutStopsTheStartWithAClearMessage() {
        assertThatThrownBy(() -> EtlOrchestrator.requireLimitsBelowTheRowTimeout(
            List.of(output("slow-output", true, ROW_TIMEOUT)), ROW_TIMEOUT))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("slow-output")
            .hasMessageContaining("PT1M")
            .hasMessageContaining("datarun.etl.row-timeout");
        assertThatThrownBy(() -> EtlOrchestrator.requireLimitsBelowTheRowTimeout(
            List.of(output("no-time", true, Duration.ZERO)), ROW_TIMEOUT))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anOutputThatIsOffIsNotChecked() {
        assertThatCode(() -> EtlOrchestrator.requireLimitsBelowTheRowTimeout(
            List.of(output("switched-off", false, Duration.ofMinutes(5))), ROW_TIMEOUT)).doesNotThrowAnyException();
    }
}
