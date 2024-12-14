//package org.nmcpye.datarun.repository;
//
//import java.util.List;
//import java.util.Optional;
//import org.nmcpye.datarun.domain.Warehouse;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.*;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
///**
// * Spring Data JPA repository for the Warehouse entity.
// */
//@Repository
//public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
//    default Optional<Warehouse> findOneWithEagerRelationships(Long id) {
//        return this.findOneWithToOneRelationships(id);
//    }
//
//    default List<Warehouse> findAllWithEagerRelationships() {
//        return this.findAllWithToOneRelationships();
//    }
//
//    default Page<Warehouse> findAllWithEagerRelationships(Pageable pageable) {
//        return this.findAllWithToOneRelationships(pageable);
//    }
//
//    @Query(
//        value = "select warehouse from Warehouse warehouse left join fetch warehouse.activity",
//        countQuery = "select count(warehouse) from Warehouse warehouse"
//    )
//    Page<Warehouse> findAllWithToOneRelationships(Pageable pageable);
//
//    @Query("select warehouse from Warehouse warehouse left join fetch warehouse.activity")
//    List<Warehouse> findAllWithToOneRelationships();
//
//    @Query("select warehouse from Warehouse warehouse left join fetch warehouse.activity where warehouse.id =:id")
//    Optional<Warehouse> findOneWithToOneRelationships(@Param("id") Long id);
//}
