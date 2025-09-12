package org.nmcpye.datarun.jpa.etl.analytics.repository;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.etl.analytics.domain.AnalyticsSource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Hamza Assada
 * @since 12/09/2025
 */
@Repository
public interface AnalyticsSourceRepository extends BaseJpaIdentifiableRepository<AnalyticsSource, Long> {

    /**
     * Finds an analytics source by its stable, unique identifier.
     *
     * @param uid The unique identifier of the source.
     * @return an Optional containing the AnalyticsSource if found.
     */
    Optional<AnalyticsSource> findByUid(String uid);
}
