package org.nmcpye.datarun.jpa.activity.repository;

import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
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
public interface ActivityRepository
    extends JpaIdentifiableRepository<Activity> {

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
