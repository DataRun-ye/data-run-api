package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.common.jpa.repository.JpaAuditableRepository;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.security.useraccess.dataform.AssignmentSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository
    extends JpaAuditableRepository<Assignment>, AssignmentRepositoryWithBagRelationships {


    List<Assignment> findAllByTeamUidIn(Collection<String> teamIds);


    //    @Query("select assignment from Assignment assignment where assignment.path like concat(:path, '%')")
    List<Assignment> findAllByPathContaining(String path);

    @Query(
        "select assignment from Assignment assignment " +
            "left join  assignment.team t " +
            "left join assignment.orgUnit ou " +
            "left join assignment.activity a " +
            "where t.uid =:team and ou.uid =:orgUnit and a.uid =:activity order by a.id Limit 1"
    )
    Optional<Assignment> findFirstByTeamAndOrgUnit(@Param("team") String team,
                                                   @Param("orgUnit") String orgUnit, @Param("activity") String activity);

    @Query(
        value = "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where assignment.activity.disabled =:disabled and " +
            "assignment.team.disabled =:disabled and user.login = ?#{authentication.name}"
    )
    List<Assignment> findAllByStatusUser(@Param("disabled") boolean disabled);

    Page<Assignment> findAllByPathIsNull(Pageable pageable);

    //// new test
    @Query("SELECT ou.id FROM Assignment ou WHERE ou.parent.id IN :parentIds")
    List<Long> findChildIdsByParentIds(@Param("parentIds") List<Long> parentIds);

    @Query(value = """
        SELECT DISTINCT assi.*
        FROM assignment assi
        WHERE assi.uid IN (
            SELECT unnest(string_to_array(acc.path, ','))
            FROM assignment acc
            WHERE acc.id IN :accessibleIds
        )
        """, nativeQuery = true)
    Page<Assignment> findWithAncestors(@Param("accessibleIds") List<Long> accessibleIds, Pageable pageable);

    @Query(value = """
        SELECT assi.*
        FROM assignment assi
        WHERE EXISTS (
            SELECT 1
            FROM assignment acc
            WHERE acc.id IN :accessibleIds
            AND assi.path LIKE CONCAT(acc.path, ',%')
        )
        """, nativeQuery = true)
    List<Assignment> findWithDescendants(@Param("accessibleIds") List<Long> accessibleIds);


    @Query(value = """
        SELECT assi.*
        FROM assignment assi
        WHERE EXISTS (
            SELECT 1
            FROM assignment acc
            WHERE acc.id IN :accessibleIds
            AND assi.path LIKE CONCAT(acc.path, ',%')
        )
        """, nativeQuery = true)
    List<AssignmentSummary> findSummariesDescendants(@Param("accessibleIds") List<Long> accessibleIds);

    @Query(value = """
        SELECT assi.*
        FROM assignment assi
        Join team t on t.id = assi.team_id
        WHERE t.uid IN :teamIds
        """, nativeQuery = true)
    Page<AssignmentSummary> findSummariesTeam(@Param("teamIds") Collection<String> teamIds, Pageable pageable);

    @Query(value = """
        SELECT assi.*
        FROM assignment assi
        Join team t on t.id = assi.team_id
        WHERE t.uid IN :teamId
        """, nativeQuery = true)
    Page<AssignmentSummary> findSummariesTeam(@Param("teamId") String teamId, Pageable pageable);

//    @Query(
//        value = "select assignment from Assignment assignment " +
//            "left join assignment.activity " +
//            "left join assignment.orgUnit " +
//            "left join assignment.team " +
//            "join assignment.team.users user " +
//            "where assignment.activity.disabled =:disabled and " +
//            "assignment.team.disabled =:disabled and user.login = ?#{authentication.name}",
//        countQuery = "select count(assignment) from Assignment assignment " +
//            "join assignment.team.users user " +
//            "where assignment.activity.disabled =:disabled and " +
//            "assignment.team.disabled =:disabled and user.login = ?#{authentication.name}"
//    )
//    Page<Assignment> findAllByStatusAndUser(@Param("disabled") boolean disabled, Pageable pageable);

    //    @Query(
//        value = "select assignment from Assignment assignment " +
//            "left join assignment.activity " +
//            "left join assignment.orgUnit " +
//            "left join assignment.team " +
//            "join assignment.team.users user " +
//            "where user.login = ?#{authentication.name}",
//        countQuery = "select count(assignment) from Assignment assignment " +
//            "join assignment.team.users user " +
//            "where user.login = ?#{authentication.name}"
//    )
//    Page<Assignment> findAllWithToOneRelationshipsByUser(Pageable pageable);

//    @Query(
//        "select assignment from Assignment assignment " +
//            "left join assignment.activity " +
//            "left join assignment.orgUnit " +
//            "left join assignment.team " +
//            "join assignment.team.users user " +
//            "where assignment.id =:id and user.login = ?#{authentication.name}"
//    )
//    Optional<Assignment> findOneWithToOneRelationshipsByUser(@Param("id") Long id);
//
//    @Query(
//        "select assignment from Assignment assignment " +
//            "left join assignment.activity " +
//            "left join assignment.orgUnit " +
//            "left join assignment.team " +
//            "join assignment.team.users user " +
//            "where assignment.uid =:uid and user.login = ?#{authentication.name}"
//    )
//    Optional<Assignment> findOneWithToOneRelationshipsByUser(@Param("uid") String uid);
}
