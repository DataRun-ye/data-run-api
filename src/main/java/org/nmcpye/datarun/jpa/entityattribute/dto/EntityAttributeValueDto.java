package org.nmcpye.datarun.jpa.entityattribute.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.entityattribute.EntityAttributeValue}
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EntityAttributeValueDto extends BaseDto {
    private String entityInstanceId;
    private String entityAttributeId;
    @Size(max = 50000)
    private String value;
}
