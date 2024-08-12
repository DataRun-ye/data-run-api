package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
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
    extends OrgUnitRepositoryWithBagRelationships, IdentifiableRelationalRepository<OrgUnit> {

    default Page<OrgUnit> findAssignedWithEagerRelation(Pageable pageable) {
        return this.fetchBagRelationships(this.findAllWithEagerRelation(pageable));
    }

    default Page<OrgUnit> findAssignedByStatusWithEagerRelation(boolean disabled, Pageable pageable) {
        return this.fetchBagRelationships(this.findAssignedByStatus(disabled, pageable));
    }

    default List<OrgUnit> findAssignedWithEagerRelation() {
        return this.fetchBagRelationships(this.findAllWithEagerRelation());
    }

    default Optional<OrgUnit> findAssignedByUidWithEagerRelation(String uid) {
        return this.fetchBagRelationships(this.findOneByUidEager(uid));
    }

    //
    @Query(
        value = "select distinct orgUnit from OrgUnit orgUnit " +
            "left join fetch orgUnit.parent " +
            "join orgUnit.assignments assignments " +
            "where assignments.activity.disabled=:disabled and " +
            "assignments.team.disabled=:disabled " +
            "and assignments.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(distinct orgUnit) from OrgUnit orgUnit " +
            "join orgUnit.assignments assignments " +
//            "join Assignment assignment ON assignment.orgUnit = orgUnit " +
            "where assignments.activity.disabled=:disabled and " +
            "assignments.team.disabled=:disabled " +
            "and assignments.team.userInfo.login = ?#{authentication.name}"
    )
    Page<OrgUnit> findAssignedByStatus(boolean disabled, Pageable pageable);

    @Query(
        value = "select distinct orgUnit from OrgUnit orgUnit " +
            "left join fetch orgUnit.parent " +
            "join orgUnit.assignments assignments " +
            "where assignments.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(distinct orgUnit) from OrgUnit orgUnit " +
            "join orgUnit.assignments assignments " +
            "where assignments.team.userInfo.login = ?#{authentication.name}"
    )
    Page<OrgUnit> findAllWithEagerRelation(Pageable pageable);

    @Query(
        value = "select distinct orgUnit from OrgUnit orgUnit " +
            "left join fetch orgUnit.parent " /*+
            "join Assignment assignment ON assignment.orgUnit = orgUnit " +
            "where assignment.team.userInfo.login = ?#{authentication.name}"*/
    )
    List<OrgUnit> findAllWithEagerRelation();

    @Query(
        value = "select distinct orgUnit from OrgUnit orgUnit " +
            "left join fetch orgUnit.parent " /*+
            "join Assignment assignment ON assignment.orgUnit = orgUnit " +
            "where orgUnit.uid =:uid and assignment.team.userInfo.login = ?#{authentication.name}"*/
    )
    Optional<OrgUnit> findOneByUidEager(@Param("uid") String uid);
}
