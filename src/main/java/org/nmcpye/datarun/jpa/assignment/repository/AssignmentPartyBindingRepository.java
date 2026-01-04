package org.nmcpye.datarun.jpa.assignment.repository;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.nmcpye.datarun.jpa.assignment.AssignmentPartyBinding;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/// Spring Data JPA repository for the AssignmentPartyBinding entity.
///
/// @author Hamza Assada 28/12/2025
@Repository
public interface AssignmentPartyBindingRepository extends BaseJpaRepository<AssignmentPartyBinding, UUID> {
//
//    @Query("""
//            SELECT b FROM AssignmentPartyBinding b
//            WHERE b.assignmentId = :assignmentId
//              AND b.name = :roleName
//              AND (b.vocabularyId = :vocabularyId OR b.vocabularyId IS NULL)
//            ORDER BY b.vocabularyId NULLS LAST
//        """)
//    List<AssignmentPartyBinding> findBindingsForResolution(
//        @Param("assignmentId") String assignmentId,
//        @Param("roleName") String roleName,
//        @Param("vocabularyId") String vocabularyId
//    );

    List<AssignmentPartyBinding> findByAssignmentIdIn(Collection<String> assignmentIds);

    Collection<AssignmentPartyBinding> findByAssignmentId(String assignmentId);

    Optional<AssignmentPartyBinding> findByUid(String uid);

    List<AssignmentPartyBinding> findByAssignmentIdOrAssignmentUid(String assignmentId, String assignmentUid);

    /**
     * Checks if any AssignmentPartyBinding entities exist that reference the given partySetId.
     *
     * @param partySetId The ID of the PartySet to check for.
     * @return true if at least one binding exists, false otherwise.
     */
    boolean existsByPartySetId(UUID partySetId);

    boolean existsByPartySetUid(String partySetUid);
}
