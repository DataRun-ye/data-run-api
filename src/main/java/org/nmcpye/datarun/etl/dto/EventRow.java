package org.nmcpye.datarun.etl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author Hamza Assada
 * @since 10/02/2026
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class EventRow {
    private String eventId;
    private String eventType;
    private String eventCeId;
    private String parentEventId;
}
