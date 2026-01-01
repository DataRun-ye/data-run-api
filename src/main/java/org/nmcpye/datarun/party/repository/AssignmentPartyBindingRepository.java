package org.nmcpye.datarun.party.repository;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.nmcpye.datarun.party.entities.AssignmentPartyBinding;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/// Spring Data JPA repository for the AssignmentPartyBinding entity.
///
/// @author Hamza Assada 28/12/2025
@Repository
public interface AssignmentPartyBindingRepository extends BaseJpaRepository<AssignmentPartyBinding, UUID> {

    @Query("""
            SELECT b FROM AssignmentPartyBinding b
            WHERE b.assignmentId = :assignmentId
              AND b.name = :roleName
              AND (b.vocabularyId = :vocabularyId OR b.vocabularyId IS NULL)
            ORDER BY b.vocabularyId NULLS LAST
        """)
    List<AssignmentPartyBinding> findBindingsForResolution(
        @Param("assignmentId") String assignmentId,
        @Param("roleName") String roleName,
        @Param("vocabularyId") String vocabularyId
    );

    List<AssignmentPartyBinding> findByAssignmentId(String assignmentId);
}
