package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.ItnsVillageHousesDetail;
import org.nmcpye.datarun.repository.ItnsVillageHousesDetailRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the ItnsVillageHousesDetail entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ItnsVillageHousesDetailRepositoryCustom
    extends ItnsVillageHousesDetailRepository {
    void deleteByUid(String uid);

    Optional<ItnsVillageHousesDetail> findByUid(String uid);
}
