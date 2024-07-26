package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.ChvSupply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the ChvSupply entity.
 */
@Repository
public interface ChvSupplyRepositoryCustom extends IdentifiableRepository<ChvSupply> {
    Optional<ChvSupply> findByUid(String uid);

    default Optional<ChvSupply> findOneWithEagerRelationshipsByUser(Long id) {
        return this.findOneWithToOneRelationshipsByUser(id);
    }

    default Page<ChvSupply> findAllWithEagerRelationshipsByUser(Pageable pageable) {
        return this.findAllWithToOneRelationshipsByUser(pageable);
    }

    @Query(
        value = "select chvSupply from ChvSupply chvSupply " +
            "left join chvSupply.team " +
            "left join chvSupply.activity " +
            "where chvSupply.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(chvSupply) from ChvSupply chvSupply " +
            "where chvSupply.team.userInfo.login = ?#{authentication.name}"
    )
    Page<ChvSupply> findAllByUser(Pageable pageable);


    @Query(
        value = "select chvSupply from ChvSupply chvSupply " +
            "left join fetch chvSupply.team " +
            "left join chvSupply.activity " +
            "where chvSupply.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(chvSupply) from ChvSupply chvSupply " +
            "where chvSupply.team.userInfo.login = ?#{authentication.name}"
    )
    Page<ChvSupply> findAllWithToOneRelationshipsByUser(Pageable pageable);

    @Query("select chvSupply from ChvSupply chvSupply " +
        "left join fetch chvSupply.team " +
        "left join chvSupply.activity " +
        "where chvSupply.id =:id and chvSupply.team.userInfo.login = ?#{authentication.name}")
    Optional<ChvSupply> findOneWithToOneRelationshipsByUser(@Param("id") Long id);
}
