package org.nmcpye.datarun.etl.service.impl;

import org.nmcpye.datarun.etl.service.EventProcessorService;
import org.nmcpye.datarun.outbox.dto.OutboxDto;
import org.nmcpye.datarun.outbox.processing.ChangeOutput;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * The legacy output of the outbox's changes: their projection into {@code analytics.tall_canonical} and
 * {@code analytics.events} ({@link EventProcessorService}). Always on; it keeps no limit of its own, so its statements
 * run within the ETL's row timeout. A failure is the runtime's: the outbox row is tried again, and is dead past its
 * attempts. It rolls back this output's savepoint only, so it never holds back another output. Applied after the
 * submission feed.
 */
@Component
@Order(2)
public class TallAndEventsOutput implements ChangeOutput {

    private final EventProcessorService processor;

    public TallAndEventsOutput(EventProcessorService processor) {
        this.processor = processor;
    }

    @Override
    public String name() {
        return "tall-and-events";
    }

    @Override
    public boolean isOn() {
        return true;
    }

    @Override
    public Optional<Duration> timeLimit() {
        return Optional.empty();
    }

    @Override
    public void apply(OutboxDto change, long deadlineNanos) {
        processor.processEvent(change);
    }
}
