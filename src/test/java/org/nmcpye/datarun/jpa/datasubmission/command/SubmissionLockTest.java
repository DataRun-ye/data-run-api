package org.nmcpye.datarun.jpa.datasubmission.command;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** The submission lock bounds the transaction's lock waits before it reads anything, then locks the submission. */
class SubmissionLockTest {

    private final DataSubmissionRepository submissions = mock(DataSubmissionRepository.class);
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);

    @Test
    void theBoundComesFirstThenTheStoredSubmissionIsReadUnderTheLock() {
        DataSubmission stored = new DataSubmission();
        when(submissions.findByUidForWrite("storedSub01")).thenReturn(Optional.of(stored));

        DataSubmission locked = new SubmissionLock(submissions, jdbc, Duration.ofSeconds(5)).take("storedSub01");

        assertThat(locked).isSameAs(stored);
        InOrder order = inOrder(jdbc, submissions);
        ArgumentCaptor<MapSqlParameterSource> bound = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        order.verify(jdbc).queryForObject(eq("SELECT set_config('lock_timeout', :timeout, true)"), bound.capture(),
            eq(String.class));
        order.verify(submissions).findByUidForWrite("storedSub01");
        assertThat(bound.getValue().getValue("timeout")).isEqualTo("5000ms");
    }

    @Test
    void aSubmissionNotStoredIsNull() {
        assertThat(new SubmissionLock(submissions, jdbc, Duration.ofSeconds(5)).take("newSub00001")).isNull();
    }

    /** An item without a uid locks nothing, and its waits are bounded all the same. */
    @Test
    void noUidLocksNothing() {
        assertThat(new SubmissionLock(submissions, jdbc, Duration.ofSeconds(5)).take(null)).isNull();

        verify(jdbc).queryForObject(any(String.class), any(MapSqlParameterSource.class), eq(String.class));
        verifyNoInteractions(submissions);
    }

    /** A bound under a millisecond is one: 0 would turn the bound off. */
    @Test
    void theBoundIsAtLeastOneMillisecond() {
        new SubmissionLock(submissions, jdbc, Duration.ofNanos(1)).take("newSub00001");

        ArgumentCaptor<MapSqlParameterSource> bound = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForObject(any(String.class), bound.capture(), eq(String.class));
        assertThat(bound.getValue().getValue("timeout")).isEqualTo("1ms");
    }
}
