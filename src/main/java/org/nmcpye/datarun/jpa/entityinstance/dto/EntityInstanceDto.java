package org.nmcpye.datarun.jpa.entityinstance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.entityinstance.EntityInstance}
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
public class EntityInstanceDto extends BaseDto {
    private String name;
    private EntityInstance.EntityStatus status = EntityInstance.EntityStatus.ACTIVE;
    @NotNull
    private BaseDto entityType;
    private Instant createdAtClient;
    private Instant updatedAtClient;
    @NotNull
    private Map<String, Object> identityAttributes = new HashMap<>();
}
