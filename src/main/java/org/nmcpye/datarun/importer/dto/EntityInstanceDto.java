package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.entityinstance.EntityInstance}
 */
@AllArgsConstructor
@Getter
@Setter
public class EntityInstanceDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final UUID uuid;
    private final EntityInstance.EntityStatus status;
    private final Instant createdAtClient;
    private final Instant updatedAtClient;
    private final Set<EntityAttributeValueDto> entityAttributeValues;
}
