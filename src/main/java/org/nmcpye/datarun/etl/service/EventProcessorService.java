package org.nmcpye.datarun.etl.service;

import org.nmcpye.datarun.etl.dto.OutboxDto;

import java.util.UUID;

public interface EventProcessorService {
    void processEvent(UUID etlRunId, OutboxDto outbox, UUID ingestId) throws Exception;
}
