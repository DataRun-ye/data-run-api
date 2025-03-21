//package org.nmcpye.datarun.repository;
//
//import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
///**
// * Spring Data JPA repository for the OrgUnit entity.
// */
//@Repository
//public interface OrgUnitRepository extends JpaRepository<OrgUnit, Long> {
//    default Optional<OrgUnit> findOneWithEagerRelationships(Long id) {
//        return this.findOneWithToOneRelationships(id);
//    }
//
//    default List<OrgUnit> findAllWithEagerRelationships() {
//        return this.findAllWithToOneRelationships();
//    }
//
//    default Page<OrgUnit> findAllWithEagerRelationships(Pageable pageable) {
//        return this.findAllWithToOneRelationships(pageable);
//    }
//
//    @Query(
//        value = "select orgUnit from OrgUnit orgUnit",
//        countQuery = "select count(orgUnit) from OrgUnit orgUnit"
//    )
//    Page<OrgUnit> findAllWithToOneRelationships(Pageable pageable);
//
//    @Query("select orgUnit from OrgUnit orgUnit")
//    List<OrgUnit> findAllWithToOneRelationships();
//
//    @Query(
//        "select orgUnit from OrgUnit orgUnit where orgUnit.id =:id"
//    )
//    Optional<OrgUnit> findOneWithToOneRelationships(@Param("id") Long id);
//}
