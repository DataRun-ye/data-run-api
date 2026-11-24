package org.nmcpye.datarun.jpa.datasubmission.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.NoSuchElementException;
import java.util.Optional;

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
    private SubmissionCommandService service;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transaction);
        service = new SubmissionCommandService(
            mock(SubmissionUploads.class), submissionService, submissions, transactionManager);
    }

    @Test
    void anAdminDeleteMarksTheSubmissionDeletedInOneTransaction() {
        DataSubmission stored = new DataSubmission();
        stored.setUid("storedSub01");
        when(submissions.findByUid("storedSub01")).thenReturn(Optional.of(stored));

        service.delete(new SubmissionCommand.Delete(actor(true), "storedSub01"));

        verify(submissionService).softDelete(stored);
        verify(transactionManager).commit(transaction);
    }

    @Test
    void deletingAnUnknownUidWritesNothing() {
        when(submissions.findByUid("noSuchSub01")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
            () -> service.delete(new SubmissionCommand.Delete(actor(true), "noSuchSub01")));

        verify(submissionService, never()).softDelete(any());
        verify(transactionManager).rollback(transaction);
    }

    @Test
    void onlyAnAdminMayDelete() {
        assertThrows(AccessDeniedException.class,
            () -> service.delete(new SubmissionCommand.Delete(actor(false), "storedSub01")));

        verifyNoInteractions(submissions, submissionService, transactionManager);
    }

    private static SubmissionActor actor(boolean admin) {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.isSuper()).thenReturn(admin);
        when(user.getId()).thenReturn("01JUSER00000000000000000AA");
        when(user.getUsername()).thenReturn(admin ? "admin" : "field-user");
        return SubmissionActor.of(user);
    }
}
