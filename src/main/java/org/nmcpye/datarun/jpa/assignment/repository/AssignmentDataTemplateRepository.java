package org.nmcpye.datarun.jpa.assignment.repository;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.nmcpye.datarun.jpa.assignment.AssignmentDataTemplateEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/// Spring Data JPA repository for the AssignmentPartyBinding entity.
///
/// @author Hamza Assada 28/12/2025
@Repository
public interface AssignmentDataTemplateRepository
    extends BaseJpaRepository<AssignmentDataTemplateEntity, UUID> {

    List<AssignmentDataTemplateEntity> findByAssignmentIdIn(Collection<String> assignmentIds);

    Collection<AssignmentDataTemplateEntity> findByAssignmentId(String assignmentId);

    @Query(value = """
        WITH user_principals AS (
          SELECT :userId::text as principal
          UNION ALL SELECT unnest(:principalSet) -- pass teams/group ids as principalSet
        ),
        user_roles AS (
          SELECT am.role
          FROM assignment_member am
          WHERE am.assignment_id = :assignmentId AND (am.member_id = :userId OR am.member_id = ANY(:teamIds))
        )
        SELECT DISTINCT adt.data_template_id
        FROM assignment_data_template adt
        WHERE adt.assignment_id = :assignmentId
          AND (
            -- exact principal id match
            (adt.principal_id IS NOT NULL AND adt.principal_id = :userId)
            OR
            -- principal_type+principal_id match (team/user_group)
            (adt.principal_type IS NOT NULL AND adt.principal_id IS NOT NULL AND adt.principal_id = ANY(:principalIds))
            OR
            -- principal_role match (assignment_member.role)
            (adt.principal_role IS NOT NULL AND adt.principal_role = ANY(:userRoles))
            OR
            -- global (no principal) entries (visible to all)
            (adt.principal_id IS NULL AND adt.principal_role IS NULL AND adt.principal_type IS NULL)
          );
        """, nativeQuery = true)
    List<String> findAllowedTemplates(@Param("assignmentId") String assignmentId,
                                      @Param("userId") String userId,
                                      @Param("principalIds") Collection<String> principalIds,
                                      @Param("teamIds") Collection<String> teamIds,
                                      @Param("userRoles") Collection<String> userRoles);
}
