package org.nmcpye.datarun.importer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.entityattribute.EntityAttributeValue}
 */
@AllArgsConstructor
@Getter
@Setter
public class EntityAttributeValueDto extends AbstractBaseDto {
    private final EntityInstanceDto entityInstance;
    private final String entityAttributeUid;
    private final String value;
}
