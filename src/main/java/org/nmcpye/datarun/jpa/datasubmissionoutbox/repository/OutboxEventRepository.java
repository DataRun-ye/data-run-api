package org.nmcpye.datarun.jpa.datasubmissionoutbox.repository;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
public interface OutboxEventRepository
    extends BaseJpaIdentifiableRepository<OutboxEvent, Long>, OutboxEventClaimsRepository {
}
