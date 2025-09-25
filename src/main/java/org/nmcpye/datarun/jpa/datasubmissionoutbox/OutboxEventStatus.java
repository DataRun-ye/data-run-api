package org.nmcpye.datarun.jpa.datasubmissionoutbox;

/**
 * @author Hamza Assada
 * @since 15/08/2025
 */
public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    DONE,
    FAILED,
    DLQ
}
