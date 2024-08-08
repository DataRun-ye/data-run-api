package org.nmcpye.datarun.repository;

import java.util.List;
import java.util.Optional;
import org.nmcpye.datarun.domain.OrganizationUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OrganizationUnit entity.
 */
@Repository
public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, Long> {
    default Optional<OrganizationUnit> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<OrganizationUnit> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<OrganizationUnit> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select organizationUnit from OrganizationUnit organizationUnit left join fetch organizationUnit.level",
        countQuery = "select count(organizationUnit) from OrganizationUnit organizationUnit"
    )
    Page<OrganizationUnit> findAllWithToOneRelationships(Pageable pageable);

    @Query("select organizationUnit from OrganizationUnit organizationUnit left join fetch organizationUnit.level")
    List<OrganizationUnit> findAllWithToOneRelationships();

    @Query(
        "select organizationUnit from OrganizationUnit organizationUnit left join fetch organizationUnit.level where organizationUnit.id =:id"
    )
    Optional<OrganizationUnit> findOneWithToOneRelationships(@Param("id") Long id);
}
