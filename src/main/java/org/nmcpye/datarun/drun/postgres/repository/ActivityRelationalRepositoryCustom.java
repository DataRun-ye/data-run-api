package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Activity entity.
 */
@Repository
public interface ActivityRelationalRepositoryCustom
    extends IdentifiableRelationalRepository<Activity> {

    default Optional<Activity> findOneWithEagerRelationshipsByUser(Long id) {
        return this.findOneWithToOneRelationshipsByUser(id);
    }

    @Query(
        value = "select activity from Activity activity " +
            "join Assignment assignment on assignment.activity = activity " +
            "join Team team on assignment.team = team " +
            "join assignment.team.users u " +
            "where u.login = ?#{authentication.name} and activity.disabled = false",
        countQuery = "select count(activity) from Activity activity " +
            "join Assignment assignment on assignment.activity = activity " +
            "join Team team on assignment.team = team " +
            "join assignment.team.users u " +
            "where u.login = ?#{authentication.name} and activity.disabled = false"
    )
    Page<Activity> findAllByUser(Pageable pageable);

    @Query("select activity from Activity activity " +
        "left join fetch activity.project where activity.id =:id")
    Optional<Activity> findOneWithToOneRelationshipsByUser(@Param("id") Long id);
}
