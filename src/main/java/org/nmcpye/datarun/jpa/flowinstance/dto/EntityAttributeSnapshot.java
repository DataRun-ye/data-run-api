package org.nmcpye.datarun.jpa.flowinstance.dto;

import java.time.Instant;

/**
 * DTO: Entity with latest attributes
 *
 * @author Hamza Assada 10/06/2025 <7amza.it@gmail.com>
 */
public interface EntityAttributeSnapshot {
    String getEntityInstanceId();

    String getKey();

    String getValue();

    Instant getLastModifiedDate();
}
