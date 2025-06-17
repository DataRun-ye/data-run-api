package org.nmcpye.datarun.jpa.entityattribute.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.entityattribute.EntityAttributeType}
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EntityAttributeTypeDto extends BaseDto {
    @NotNull
    private String name;
    private String shortName;
    @NotNull
    private ValueType valueType;
    @Size(max = 2000)
    private String description;
    private BaseDto optionSet;
    private Boolean displayOnNonSource = false;
}
