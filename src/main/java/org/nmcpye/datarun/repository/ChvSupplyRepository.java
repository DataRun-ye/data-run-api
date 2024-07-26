package org.nmcpye.datarun.repository;

import java.util.List;
import java.util.Optional;
import org.nmcpye.datarun.domain.ChvSupply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ChvSupply entity.
 */
@Repository
public interface ChvSupplyRepository extends JpaRepository<ChvSupply, Long> {
    default Optional<ChvSupply> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ChvSupply> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ChvSupply> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select chvSupply from ChvSupply chvSupply left join fetch chvSupply.activity left join fetch chvSupply.team",
        countQuery = "select count(chvSupply) from ChvSupply chvSupply"
    )
    Page<ChvSupply> findAllWithToOneRelationships(Pageable pageable);

    @Query("select chvSupply from ChvSupply chvSupply left join fetch chvSupply.activity left join fetch chvSupply.team")
    List<ChvSupply> findAllWithToOneRelationships();

    @Query(
        "select chvSupply from ChvSupply chvSupply left join fetch chvSupply.activity left join fetch chvSupply.team where chvSupply.id =:id"
    )
    Optional<ChvSupply> findOneWithToOneRelationships(@Param("id") Long id);
}
