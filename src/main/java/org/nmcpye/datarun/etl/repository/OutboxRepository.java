package org.nmcpye.datarun.etl.repository;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.nmcpye.datarun.etl.entity.Outbox;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * JPA repository for outbox_processing journal.
 * Note: writes to this table are done from service code; ensure transactional boundaries.
 */
@Repository
public interface OutboxRepository extends BaseJpaRepository<Outbox, Long> {
    List<Outbox> findBySubmissionSerialNumberIn(Collection<Long> serialNumbers);
}
