package org.nmcpye.datarun.etl.service;

import org.nmcpye.datarun.outbox.dto.OutboxDto;

import java.util.UUID;

public interface EventProcessorService {
    /**
     * Projects one claimed outbox row and records its success inside the caller's transaction; any failure
     * propagates so the caller can roll back before recording it.
     */
    void processEvent(UUID etlRunId, OutboxDto outbox, UUID ingestId);
}
