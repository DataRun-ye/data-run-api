package org.nmcpye.datarun.drun.postgres.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.nmcpye.datarun.domain.ItnsVillage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class ItnsVillageRepositoryWithBagRelationshipsImpl
    implements ItnsVillageRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String ASSIGNMENTS_PARAMETER = "itnsVillages";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<ItnsVillage> fetchBagRelationships(Optional<ItnsVillage> itnsVillage) {
        return itnsVillage.map(this::fetchHousesDetails);
    }

    @Override
    public Page<ItnsVillage> fetchBagRelationships(Page<ItnsVillage> itnsVillages) {
        return new PageImpl<>(fetchBagRelationships(itnsVillages.getContent()), itnsVillages.getPageable(), itnsVillages.getTotalElements());
    }

    @Override
    public List<ItnsVillage> fetchBagRelationships(List<ItnsVillage> itnsVillages) {
        return Optional.of(itnsVillages).map(this::fetchHousesDetails).orElse(Collections.emptyList());
    }

    ItnsVillage fetchHousesDetails(ItnsVillage result) {
        return entityManager
            .createQuery(
                "select itnsVillage from ItnsVillage itnsVillage " +
                    "left join fetch itnsVillage.houseDetails " +
                    "where itnsVillage.id = :id",
                ItnsVillage.class
            )
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<ItnsVillage> fetchHousesDetails(List<ItnsVillage> itnsVillages) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, itnsVillages.size()).forEach(index -> order.put(itnsVillages.get(index).getId(), index));
        List<ItnsVillage> result = entityManager
            .createQuery(
                "select itnsVillage from ItnsVillage itnsVillage " +
                    "left join fetch itnsVillage.houseDetails " +
                    "where itnsVillage in :itnsVillages",
                ItnsVillage.class
            )
            .setParameter(ASSIGNMENTS_PARAMETER, itnsVillages)
            .getResultList();
        Collections.sort(result, (o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
