package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.WarehouseTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the WarehouseTransaction entity.
 */
@Repository
public interface WarehouseTransactionRepositoryCustom
    extends IdentifiableRepository<WarehouseTransaction> {

    Optional<WarehouseTransaction> findByUid(String uid);

    default Optional<WarehouseTransaction> findOneWithEagerRelationshipsByUser(Long id) {
        return this.findOneWithToOneRelationshipsByUser(id);
    }

    default Page<WarehouseTransaction> findAllWithEagerRelationshipsByUser(Pageable pageable) {
        return this.findAllWithToOneRelationshipsByUser(pageable);
    }

    @Query(
        value = "select warehouseTransaction from WarehouseTransaction warehouseTransaction " +
            "left join warehouseTransaction.item " +
            "left join warehouseTransaction.sourceWarehouse " +
            "left join warehouseTransaction.team " +
            "left join warehouseTransaction.warehouse " +
            "left join warehouseTransaction.activity " +
            "where warehouseTransaction.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(warehouseTransaction) from WarehouseTransaction warehouseTransaction " +
            "where warehouseTransaction.team.userInfo.login = ?#{authentication.name}"
    )
    Page<WarehouseTransaction> findAllByUser(Pageable pageable);

    @Query(
        value = "select warehouseTransaction from WarehouseTransaction warehouseTransaction " +
            "left join fetch warehouseTransaction.item " +
            "left join fetch warehouseTransaction.sourceWarehouse " +
            "left join fetch warehouseTransaction.team " +
            "left join fetch warehouseTransaction.warehouse " +
            "left join fetch warehouseTransaction.activity " +
            "where warehouseTransaction.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(warehouseTransaction) from WarehouseTransaction warehouseTransaction " +
            "where warehouseTransaction.team.userInfo.login = ?#{authentication.name}"
    )
    Page<WarehouseTransaction> findAllWithToOneRelationshipsByUser(Pageable pageable);

//    @Query(
//        "select warehouseTransaction from WarehouseTransaction warehouseTransaction " +
//                "left join fetch warehouseTransaction.item " +
//                "left join fetch warehouseTransaction.sourceWarehouse " +
//                "left join fetch warehouseTransaction.team " +
//                "left join fetch warehouseTransaction.warehouse " +
//                "left join fetch warehouseTransaction.activity " +
//                "where warehouseTransaction.team.userInfo.login = ?#{authentication.name}"
//    )
//    List<WarehouseTransaction> findAllWithToOneRelationshipsByUser();

    @Query(
        "select warehouseTransaction from WarehouseTransaction warehouseTransaction " +
            "left join fetch warehouseTransaction.item " +
            "left join fetch warehouseTransaction.sourceWarehouse " +
            "left join fetch warehouseTransaction.team " +
            "left join fetch warehouseTransaction.warehouse " +
            "left join fetch warehouseTransaction.activity " +
            "where warehouseTransaction.id =:id and warehouseTransaction.team.userInfo.login = ?#{authentication.name}"
    )
    Optional<WarehouseTransaction> findOneWithToOneRelationshipsByUser(@Param("id") Long id);
}
