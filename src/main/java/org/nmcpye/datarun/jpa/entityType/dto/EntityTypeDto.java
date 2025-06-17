package org.nmcpye.datarun.jpa.entityType.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;
import org.nmcpye.datarun.jpa.entityattribute.EntityAttributeInstance;

import java.util.LinkedList;
import java.util.List;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.entityType.EntityType}
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EntityTypeDto extends BaseDto {
    private String name;
    private List<EntityAttributeInstance> entityAttributes = new LinkedList<>();
}
