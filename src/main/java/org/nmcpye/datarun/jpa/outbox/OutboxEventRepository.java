package org.nmcpye.datarun.jpa.outbox;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
public interface OutboxEventRepository
    extends BaseJpaIdentifiableRepository<OutboxEvent, Long>, OutboxEventClaimsRepository {
}
