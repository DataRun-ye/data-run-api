package org.nmcpye.datarun.etl.admin;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.metrics.EtlHeartbeat;
import org.nmcpye.datarun.etl.metrics.EtlMetrics;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.etl.orchestrator.EtlRunResult;
import org.nmcpye.datarun.submission.feed.publisher.SubmissionPublisher;
import org.mockito.InOrder;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Which polls ping the heartbeat monitor (only those that succeed), and when the feed's publisher gets the work behind
 * the live backlog.
 */
class ScheduledOrchestratorRunnerTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final EtlOrchestrator orchestrator = mock(EtlOrchestrator.class);
    private final EtlRunLock lock = mock(EtlRunLock.class);
    private final EtlHeartbeat heartbeat = mock(EtlHeartbeat.class);
    private final SubmissionPublisher feed = mock(SubmissionPublisher.class);
    private final EtlProperties properties = new EtlProperties();
    private final ScheduledOrchestratorRunner runner = new ScheduledOrchestratorRunner(jdbc, orchestrator, lock,
        new EtlMetrics(new SimpleMeterRegistry(), mock(DataSource.class), properties), properties, heartbeat, feed);

    @Test
    void aPollThatFindsNothingDueSucceedsAndPings() {
        rowsDue(false);

        runner.scheduledRun();

        verify(heartbeat).pollSucceeded();
    }

    @Test
    void aPollThatDrainsTheOutboxSucceedsAndPings() {
        rowsDue(true);
        lockFree();
        when(orchestrator.runOnce(anyString(), anyInt())).thenReturn(new EtlRunResult(3, 3, 0, false));

        runner.scheduledRun();

        verify(heartbeat).pollSucceeded();
    }

    @Test
    void aStoppedPollDoesNotPing() {
        rowsDue(true);
        lockFree();
        when(orchestrator.runOnce(anyString(), anyInt())).thenReturn(new EtlRunResult(3, 0, 1, true));

        runner.scheduledRun();

        verify(heartbeat, never()).pollSucceeded();
    }

    @Test
    void aFailedPollDoesNotPing() {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Boolean.class)))
            .thenThrow(new DataAccessResourceFailureException("database unreachable"));

        assertThatThrownBy(runner::scheduledRun).isInstanceOf(DataAccessResourceFailureException.class);

        verify(heartbeat, never()).pollSucceeded();
    }

    @Test
    void aPollSkippedForAnotherRunDoesNotPing() {
        rowsDue(true);
        when(lock.callExclusively(any())).thenReturn(Optional.empty());

        runner.scheduledRun();

        verify(heartbeat, never()).pollSucceeded();
    }

    @Test
    void withTheFeedOnAPollRunsTheFeedsBatchesOnceTheLiveBacklogIsDrainedUntilNothingIsLeft() {
        when(feed.isOn()).thenReturn(true);
        rowsDue(true, false, false, false);
        lockFree();
        when(orchestrator.runOnce(anyString(), anyInt())).thenReturn(new EtlRunResult(3, 3, 0, false));
        when(feed.publishAfterBacklog()).thenReturn(true, true, false);

        runner.scheduledRun();

        InOrder order = inOrder(orchestrator, feed);
        order.verify(orchestrator).runOnce(anyString(), anyInt());
        order.verify(feed, times(3)).publishAfterBacklog();
        verify(heartbeat).pollSucceeded();
    }

    @Test
    void withTheFeedOnALiveRowDueAgainIsClaimedBeforeTheFeedsNextBatch() {
        when(feed.isOn()).thenReturn(true);
        rowsDue(false, true, false);
        lockFree();
        when(orchestrator.runOnce(anyString(), anyInt())).thenReturn(new EtlRunResult(1, 1, 0, false));
        when(feed.publishAfterBacklog()).thenReturn(true, false);

        runner.scheduledRun();

        InOrder order = inOrder(orchestrator, feed);
        order.verify(feed).publishAfterBacklog();
        order.verify(orchestrator).runOnce(anyString(), anyInt());
        order.verify(feed).publishAfterBacklog();
    }

    @Test
    void withTheFeedOffNothingIsHandedToIt() {
        rowsDue(true);
        lockFree();
        when(orchestrator.runOnce(anyString(), anyInt())).thenReturn(new EtlRunResult(3, 3, 0, false));

        runner.scheduledRun();

        verify(feed, never()).publishAfterBacklog();
    }

    private void rowsDue(boolean due, Boolean... then) {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Boolean.class))).thenReturn(due, then);
    }

    private void rowsDue(boolean due) {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Boolean.class))).thenReturn(due);
    }

    private void lockFree() {
        when(lock.callExclusively(any())).thenAnswer(call -> Optional.of(call.<Supplier<?>>getArgument(0).get()));
    }
}
