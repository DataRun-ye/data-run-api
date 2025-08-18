package org.nmcpye.datarun.jpa.datasubmission.migration.postgre;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Hamza Assada 16/08/2025 (7amza.it@gmail.com)
 */

@Repository
public interface MigrationErrorRepository extends BaseJpaRepository<MigrationError, Long> {
    // simple repo; BaseJpaRepository provides persist/persistAllAndFlush etc.
}
