package org.nmcpye.datarun.jpa.featureflag.repository;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.nmcpye.datarun.jpa.featureflag.FeatureFlag;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Hamza Assada
 * @since 23/09/2025
 */
@Repository
public interface FeatureFlagRepository extends BaseJpaRepository<FeatureFlag, Long> {
    Optional<FeatureFlag> findByName(String name);
}
