package org.nmcpye.datarun.jpa.assignment.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentMaintenanceServiceTest {

    @Test
    void missingPathProcessingUsesFirstPageAndUpdatesOnlyDerivedColumns() {
        AssignmentRepository repository = mock(AssignmentRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));

        Assignment assignment = new Assignment();
        assignment.setId("aat-maintenance");
        assignment.setUid("S1000000001");
        Page<Assignment> first = new PageImpl<>(List.of(assignment));
        when(repository.findAllByPathIsNull(any(Pageable.class)))
            .thenReturn(first)
            .thenReturn(Page.empty());

        new AssignmentMaintenanceService(
            repository,
            entityManager,
            transactionManager
        ).updateMissingPaths();

        verify(repository).updateDerivedPath(
            "aat-maintenance",
            ",S1000000001",
            1
        );
        verify(repository, org.mockito.Mockito.times(2))
            .findAllByPathIsNull(any(Pageable.class));
        verify(repository, never()).saveAll(any());
    }

    @Test
    void forceProcessingAdvancesStablePagesAndUsesOnlyDerivedUpdates() {
        AssignmentRepository repository = mock(AssignmentRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));

        Assignment firstAssignment = assignment("aat-force-1", "S1000000001");
        Assignment secondAssignment = assignment("aat-force-2", "S1000000002");
        when(repository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(
                List.of(firstAssignment),
                PageRequest.of(0, 400),
                401
            ))
            .thenReturn(new PageImpl<>(
                List.of(secondAssignment),
                PageRequest.of(1, 400),
                401
            ));

        new AssignmentMaintenanceService(
            repository,
            entityManager,
            transactionManager
        ).forceRecomputePaths();

        verify(repository).updateDerivedPath("aat-force-1", ",S1000000001", 1);
        verify(repository).updateDerivedPath("aat-force-2", ",S1000000002", 1);
        verify(repository, org.mockito.Mockito.times(2)).findAll(any(Pageable.class));
        verify(repository, never()).saveAll(any());
    }

    private Assignment assignment(String id, String uid) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setUid(uid);
        return assignment;
    }
}
