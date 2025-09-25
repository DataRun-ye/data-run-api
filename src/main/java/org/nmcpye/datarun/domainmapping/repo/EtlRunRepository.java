package org.nmcpye.datarun.domainmapping.repo;

import org.nmcpye.datarun.domainmapping.model.EtlRun;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Hamza Assada
 * @since 23/09/2025
 */
@Repository
public interface EtlRunRepository {
    Optional<EtlRun> findNextUnprocessed();
    void markMappingProcessed(UUID etlRunId, UUID mappingRunId);
}


