package org.nmcpye.datarun.repository;

import java.util.List;
import java.util.Optional;
import org.nmcpye.datarun.domain.OrgUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OrgUnit entity.
 */
@Repository
public interface OrgUnitRepository extends JpaRepository<OrgUnit, Long> {
    default Optional<OrgUnit> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<OrgUnit> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<OrgUnit> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select orgUnit from OrgUnit orgUnit left join fetch orgUnit.level",
        countQuery = "select count(orgUnit) from OrgUnit orgUnit"
    )
    Page<OrgUnit> findAllWithToOneRelationships(Pageable pageable);

    @Query("select orgUnit from OrgUnit orgUnit left join fetch orgUnit.level")
    List<OrgUnit> findAllWithToOneRelationships();

    @Query(
        "select orgUnit from OrgUnit orgUnit left join fetch orgUnit.level where orgUnit.id =:id"
    )
    Optional<OrgUnit> findOneWithToOneRelationships(@Param("id") Long id);
}
