package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.entityattribute.EntityAttributeInstance}
 */
@AllArgsConstructor
@Getter
@Setter
public class EntityAttributeInstanceDto extends AbstractBaseDto {
    private final Boolean displayInList;
    private final Boolean mandatory;
    private final Boolean searchable;
    private final String entityTypeUid;
    @Size(max = 11)
    private final String uid;
    private final String code;
    @NotNull
    private final String name;
    private final String shortName;
    @NotNull
    private final ValueType valueType;
    @Size(max = 2000)
    private final String description;
    private final OptionSetDto optionSet;
    private final Boolean displayOnNonSource;
}
