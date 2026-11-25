package org.nmcpye.datarun.jpa.datasubmission.command;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import jakarta.persistence.PessimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.revision.RevisionContext;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.v1.intake.HeldDecisionRequest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SubmissionCommandServiceTest {

    private final DataSubmissionService submissionService = mock(DataSubmissionService.class);
    private final SubmissionLock submissionLock = mock(SubmissionLock.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final TransactionStatus transaction = mock(TransactionStatus.class);
    private final HeldSubmissionDecisions decisions = mock(HeldSubmissionDecisions.class);
    private SubmissionCommandService service;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transaction);
        service = new SubmissionCommandService(
            mock(SubmissionUploads.class), submissionService, submissionLock, transactionManager, decisions);
    }

    /** The delete reads the submission only under the submission lock, and deletes the submission it locked. */
    @Test
    void anAdminDeleteLocksTheSubmissionAndMarksItDeletedInOneTransaction() {
        DataSubmission stored = new DataSubmission();
        stored.setUid("storedSub01");
        when(submissionLock.take("storedSub01")).thenReturn(stored);

        assertEquals(SubmissionCommandService.DeleteOutcome.DELETED,
            service.delete(new SubmissionCommand.Delete(actor(true), "storedSub01")));

        verify(submissionLock).take("storedSub01");
        verify(submissionService).softDelete(stored, RevisionContext.adminDelete("01JUSER00000000000000000AA"));
        verify(transactionManager).commit(transaction);
    }

    /**
     * Another change held the submission past the submission lock's bound: nothing is deleted, the transaction rolls
     * back, one WARN line names the submission and the SQLState, and the refusal is a concurrency failure, which the
     * route answers 409 {@code error.concurrencyFailure}, however the lock's failure came out of the persistence layer.
     */
    @Test
    void aDeleteWhoseSubmissionLockTimesOutIsRefusedAsAConcurrencyFailure() {
        when(submissionLock.take("heldSub0001")).thenThrow(new PessimisticLockException("could not obtain lock",
            new SQLException("ERROR: canceling statement due to lock timeout", "55P03")));

        try (LogCapture logs = new LogCapture(SubmissionCommandService.class)) {
            ConcurrencyFailureException refused = assertThrows(ConcurrencyFailureException.class,
                () -> service.delete(new SubmissionCommand.Delete(actor(true), "heldSub0001")));

            assertEquals(PessimisticLockException.class, refused.getCause().getClass());
            assertEquals(List.of(Level.WARN), logs.events().stream().map(ILoggingEvent::getLevel).toList());
            assertTrue(logs.lines().get(0).startsWith("Submission heldSub0001 not deleted by user "
                + "01JUSER00000000000000000AA: another change held it past the submission lock's bound: "),
                logs.lines()::toString);
            assertTrue(logs.lines().get(0).contains("SQLState 55P03"), logs.lines()::toString);
            logs.assertNothingLeaks("lock timeout");
        }
        verify(submissionService, never()).softDelete(any(), any());
        verify(transactionManager).rollback(transaction);
    }

    /** A2 W6-02: a second delete used to write a second DELETE event and move deleted_at. */
    @Test
    void deletingAnAlreadyDeletedSubmissionWritesNothing() {
        DataSubmission deleted = new DataSubmission();
        deleted.setUid("deletedSub1");
        deleted.setDeleted(true);
        when(submissionLock.take("deletedSub1")).thenReturn(deleted);

        assertEquals(SubmissionCommandService.DeleteOutcome.ALREADY_DELETED,
            service.delete(new SubmissionCommand.Delete(actor(true), "deletedSub1")));

        verify(submissionService, never()).softDelete(any(), any());
        verify(transactionManager).commit(transaction);
    }

    /** A2 W6-03 and W6-04: an unknown uid used to end in a NoSuchElementException, a 500. */
    @Test
    void deletingAnUnknownUidWritesNothing() {
        when(submissionLock.take("noSuchSub01")).thenReturn(null);

        assertEquals(SubmissionCommandService.DeleteOutcome.NOT_FOUND,
            service.delete(new SubmissionCommand.Delete(actor(true), "noSuchSub01")));

        verify(submissionService, never()).softDelete(any(), any());
    }

    @Test
    void onlyAnAdminMayDelete() {
        assertThrows(AccessDeniedException.class,
            () -> service.delete(new SubmissionCommand.Delete(actor(false), "storedSub01")));

        verifyNoInteractions(submissionLock, submissionService, transactionManager);
    }

    @Test
    void onlyAnAdminMayReleaseOrDiscardHeldUploads() {
        HeldDecisionRequest selection = new HeldDecisionRequest(List.of("heldSub0001"), null, null, null);

        assertThrows(AccessDeniedException.class,
            () -> service.releaseHeld(new SubmissionCommand.ReleaseHeld(actor(false), selection)));
        assertThrows(AccessDeniedException.class,
            () -> service.discardHeld(new SubmissionCommand.DiscardHeld(actor(false), selection)));
        verifyNoInteractions(decisions);

        SubmissionActor admin = actor(true);
        service.releaseHeld(new SubmissionCommand.ReleaseHeld(admin, selection));
        service.discardHeld(new SubmissionCommand.DiscardHeld(admin, selection));
        verify(decisions).release(admin, selection);
        verify(decisions).discard(admin, selection);
    }

    private static SubmissionActor actor(boolean admin) {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.isSuper()).thenReturn(admin);
        when(user.getId()).thenReturn("01JUSER00000000000000000AA");
        when(user.getUsername()).thenReturn(admin ? "admin" : "field-user");
        return SubmissionActor.of(user);
    }
}
