package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.ItnsVillage;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface ItnsVillageRepositoryWithBagRelationships {
    Optional<ItnsVillage> fetchBagRelationships(Optional<ItnsVillage> itnsVillage);

    List<ItnsVillage> fetchBagRelationships(List<ItnsVillage> itnsVillages);

    Page<ItnsVillage> fetchBagRelationships(Page<ItnsVillage> itnsVillages);
}
