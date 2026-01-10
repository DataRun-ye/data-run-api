package org.nmcpye.etl.ledger.repo;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.nmcpye.etl.ledger.entities.EventInstance;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends BaseJpaRepository<EventInstance, String> {
}
