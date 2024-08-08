package org.nmcpye.datarun.repository;

import org.nmcpye.datarun.domain.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Assignment entity.
 */
@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    default Optional<Assignment> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Assignment> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Assignment> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select assignment from Assignment assignment left join fetch assignment.activity left join fetch assignment.orgUnit left join fetch assignment.team",
        countQuery = "select count(assignment) from Assignment assignment"
    )
    Page<Assignment> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select assignment from Assignment assignment left join fetch assignment.activity left join fetch assignment.orgUnit left join fetch assignment.team"
    )
    List<Assignment> findAllWithToOneRelationships();

    @Query(
        "select assignment from Assignment assignment left join fetch assignment.activity left join fetch assignment.orgUnit left join fetch assignment.team where assignment.id =:id"
    )
    Optional<Assignment> findOneWithToOneRelationships(@Param("id") Long id);
}
