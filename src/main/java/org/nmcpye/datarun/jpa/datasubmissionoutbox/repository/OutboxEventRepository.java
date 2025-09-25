package org.nmcpye.datarun.jpa.datasubmissionoutbox.repository;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;
import org.springframework.stereotype.Repository;

/**
 * @author Hamza Assada
 * @since 15/08/2025
 */
@Repository
public interface OutboxEventRepository
    extends BaseJpaIdentifiableRepository<OutboxEvent, Long>, OutboxEventClaimsRepository {
}
