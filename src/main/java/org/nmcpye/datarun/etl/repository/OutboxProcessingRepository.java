package org.nmcpye.datarun.etl.repository;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.nmcpye.datarun.etl.entity.OutboxProcessing;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for outbox_processing journal.
 * Note: writes to this table are done from service code; ensure transactional boundaries.
 */
@Repository
public interface OutboxProcessingRepository extends BaseJpaRepository<OutboxProcessing, Long> {

    List<OutboxProcessing> findByEtlRunId(UUID etlRunId);

    // find failures for an ETL run
//    List<OutboxProcessing> findByEtlRunIdAndStatus(UUID etlRunId, String status);

    Optional<OutboxProcessing> findByEtlRunIdAndOutboxId(UUID etlRunId, Long outboxId);
}
