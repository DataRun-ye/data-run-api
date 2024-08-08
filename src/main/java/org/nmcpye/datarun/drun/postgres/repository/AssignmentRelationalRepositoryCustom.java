package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssignmentRelationalRepositoryCustom
    extends IdentifiableRelationalRepository<Assignment> {

    default Optional<Assignment> findOneWithEagerRelationshipsByUser(Long id) {
        return this.findOneWithToOneRelationshipsByUser(id);
    }

    default Page<Assignment> findAllWithEagerRelationshipsByUser(Pageable pageable) {
        return this.findAllWithToOneRelationshipsByUser(pageable);
    }

    @Query(
        value = "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "where assignment.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(assignment) from Assignment assignment " +
            "where assignment.team.userInfo.login = ?#{authentication.name}"
    )
    Page<Assignment> findAllByUser(Pageable pageable);

    @Query(
        value = "select assignment from Assignment assignment " +
            "left join fetch assignment.activity " +
            "left join fetch assignment.orgUnit " +
            "left join fetch assignment.team " +
            "where assignment.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(assignment) from Assignment assignment " +
            "where assignment.team.userInfo.login = ?#{authentication.name}"
    )
    Page<Assignment> findAllWithToOneRelationshipsByUser(Pageable pageable);

    @Query(
        "select assignment from Assignment assignment " +
            "left join fetch assignment.activity " +
            "left join fetch assignment.orgUnit " +
            "left join fetch assignment.team " +
            "where assignment.id =:id and assignment.team.userInfo.login = ?#{authentication.name}"
    )
    Optional<Assignment> findOneWithToOneRelationshipsByUser(@Param("id") Long id);
}
