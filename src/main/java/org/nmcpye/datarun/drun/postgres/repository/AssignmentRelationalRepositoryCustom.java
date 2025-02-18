package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRelationalRepositoryCustom
    extends IdentifiableRelationalRepository<Assignment>, AssignmentRepositoryWithBagRelationships {

    default Optional<Assignment> findOneByUser(Long id) {
        return this.findOneWithToOneRelationshipsByUser(id);
    }

    default Optional<Assignment> findOneByUser(String uid) {
        return this.findOneWithToOneRelationshipsByUser(uid);
    }

    default Page<Assignment> findAllWithEagerRelationshipsByUser(Pageable pageable) {
        return this.findAllWithToOneRelationshipsByUser(pageable);
    }

    Page<Assignment> findAllByTeamIn(List<Team> teamIds, Pageable pageable);

    List<Assignment> findAllByTeamIn(List<Team> teamIds);

    //    @Query("select assignment from Assignment assignment where assignment.path like concat(:path, '%')")
    List<Assignment> findAllByPathContaining(String path);

//        @Query(
//        value = "select assignment from Assignment assignment " +
//            "left join assignment.team t " +
//            "left join assignment.orgUnit ou " +
//            "where t.id =:teamId and ou.id =:orgUnitId " +
//            "order by assignment.id limit 1"
//    )
//    Optional<Assignment> findFirstByTeamAndOrgUnit(@Param("teamId") Long team, @Param("orgUnitId") Long orgUnitUid);
//

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
            "join fetch assignment.parent p " +
            "join assignment.activity " +
            "join assignment.orgUnit " +
            "join assignment.team " +
            "where p.uid IN :parents"
    )
    List<Assignment> findAllByParentIn(@Param("parents") List<String> parents);

    @Query(
        value = "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where assignment.activity.uid =:activityUid and user.login = ?#{authentication.name}",
        countQuery = "select count(assignment) " +
            "from Assignment assignment " +
            "join assignment.team.users user " +
            "where user.login = ?#{authentication.name}"
    )
    Page<Assignment> findAllByActivityAndUser(String activityUid, Pageable pageable);

    @Query(
        value = "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where user.login = ?#{authentication.name}",
        countQuery = "select count(assignment) from Assignment assignment " +
            "join assignment.team.users user " +
            "where user.login = ?#{authentication.name}"
    )
    Page<Assignment> findAllByUser(Pageable pageable);

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


    @Query(
        value = "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where assignment.activity.disabled =:disabled and " +
            "assignment.team.disabled =:disabled and user.login = ?#{authentication.name}",
        countQuery = "select count(assignment) from Assignment assignment " +
            "join assignment.team.users user " +
            "where assignment.activity.disabled =:disabled and " +
            "assignment.team.disabled =:disabled and user.login = ?#{authentication.name}"
    )
    Page<Assignment> findAllByStatusAndUser(@Param("disabled") boolean disabled, Pageable pageable);

    @Query(
        value = "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where user.login = ?#{authentication.name}",
        countQuery = "select count(assignment) from Assignment assignment " +
            "join assignment.team.users user " +
            "where user.login = ?#{authentication.name}"
    )
    Page<Assignment> findAllWithToOneRelationshipsByUser(Pageable pageable);

    @Query(
        "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where assignment.id =:id and user.login = ?#{authentication.name}"
    )
    Optional<Assignment> findOneWithToOneRelationshipsByUser(@Param("id") Long id);

    @Query(
        "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where assignment.uid =:uid and user.login = ?#{authentication.name}"
    )
    Optional<Assignment> findOneWithToOneRelationshipsByUser(@Param("uid") String uid);
}
