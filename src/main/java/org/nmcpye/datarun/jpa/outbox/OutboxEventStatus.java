package org.nmcpye.datarun.jpa.outbox;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    DONE,
    FAILED
}
