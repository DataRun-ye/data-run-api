package org.nmcpye.datarun.jpa.datasubmission.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.revision.RevisionContext;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.v1.intake.HeldDecisionRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SubmissionCommandServiceTest {

    private final DataSubmissionService submissionService = mock(DataSubmissionService.class);
    private final DataSubmissionRepository submissions = mock(DataSubmissionRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final TransactionStatus transaction = mock(TransactionStatus.class);
    private final HeldSubmissionDecisions decisions = mock(HeldSubmissionDecisions.class);
    private SubmissionCommandService service;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transaction);
        service = new SubmissionCommandService(
            mock(SubmissionUploads.class), submissionService, submissions, transactionManager, decisions);
    }

    @Test
    void anAdminDeleteMarksTheSubmissionDeletedInOneTransaction() {
        DataSubmission stored = new DataSubmission();
        stored.setUid("storedSub01");
        when(submissions.findByUid("storedSub01")).thenReturn(Optional.of(stored));

        assertEquals(SubmissionCommandService.DeleteOutcome.DELETED,
            service.delete(new SubmissionCommand.Delete(actor(true), "storedSub01")));

        verify(submissionService).softDelete(stored, RevisionContext.adminDelete("01JUSER00000000000000000AA"));
        verify(transactionManager).commit(transaction);
    }

    /** A2 W6-02: a second delete used to write a second DELETE event and move deleted_at. */
    @Test
    void deletingAnAlreadyDeletedSubmissionWritesNothing() {
        DataSubmission deleted = new DataSubmission();
        deleted.setUid("deletedSub1");
        deleted.setDeleted(true);
        when(submissions.findByUid("deletedSub1")).thenReturn(Optional.of(deleted));

        assertEquals(SubmissionCommandService.DeleteOutcome.ALREADY_DELETED,
            service.delete(new SubmissionCommand.Delete(actor(true), "deletedSub1")));

        verify(submissionService, never()).softDelete(any(), any());
        verify(transactionManager).commit(transaction);
    }

    /** A2 W6-03 and W6-04: an unknown uid used to end in a NoSuchElementException, a 500. */
    @Test
    void deletingAnUnknownUidWritesNothing() {
        when(submissions.findByUid("noSuchSub01")).thenReturn(Optional.empty());

        assertEquals(SubmissionCommandService.DeleteOutcome.NOT_FOUND,
            service.delete(new SubmissionCommand.Delete(actor(true), "noSuchSub01")));

        verify(submissionService, never()).softDelete(any(), any());
    }

    @Test
    void onlyAnAdminMayDelete() {
        assertThrows(AccessDeniedException.class,
            () -> service.delete(new SubmissionCommand.Delete(actor(false), "storedSub01")));

        verifyNoInteractions(submissions, submissionService, transactionManager);
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
