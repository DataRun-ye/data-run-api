package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.OrgUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrgUnitRelationalRepositoryCustom
    extends IdentifiableRelationalRepository<OrgUnit> {


    @Query(
        value = "select distinct orgUnit from OrgUnit orgUnit " +
            "join Assignment assignment ON assignment.orgUnit = orgUnit " +
            "where assignment.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(distinct orgUnit) from OrgUnit orgUnit " +
            "join Assignment assignment ON assignment.orgUnit = orgUnit " +
            "where assignment.team.userInfo.login = ?#{authentication.name}"
    )
    Page<OrgUnit> findAssigned(Pageable pageable);

    @Query(
        value = "select distinct orgUnit from OrgUnit orgUnit " +
            "join Assignment assignment ON assignment.orgUnit = orgUnit " +
            "where assignment.activity.disabled=:disabled and " +
            "assignment.team.disabled=:disabled and assignment.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(distinct orgUnit) from OrgUnit orgUnit " +
            "join Assignment assignment ON assignment.orgUnit = orgUnit " +
            "where assignment.activity.disabled=:disabled and " +
            "assignment.team.disabled=:disabled and assignment.team.userInfo.login = ?#{authentication.name}"
    )
    Page<OrgUnit> findAssignedByStatus(boolean disabled, Pageable pageable);

    @Query(
        value = "select distinct orgUnit from OrgUnit orgUnit " +
            "join Assignment assignment ON assignment.orgUnit = orgUnit " +
            "where assignment.team.userInfo.login = ?#{authentication.name}"
    )
    List<OrgUnit> findAssigned();

    @Query(
        value = "select distinct orgUnit from OrgUnit orgUnit " +
            "join Assignment assignment ON assignment.orgUnit = orgUnit " +
            "where orgUnit.uid =:uid and assignment.team.userInfo.login = ?#{authentication.name}"
    )
    Optional<OrgUnit> findAssignedByUid(@Param("uid") String uid);
}
