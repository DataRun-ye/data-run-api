package org.nmcpye.datarun.etl.service;

import org.nmcpye.datarun.outbox.dto.OutboxDto;


public interface EventProcessorService {
    /**
     * Projects one claimed outbox row into tall and events, inside the caller's transaction; any failure propagates.
     * The row's outcome is the caller's to record.
     */
    void processEvent(OutboxDto outbox);
}
