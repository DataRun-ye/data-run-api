package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.datatemplateelement.enumeration.ReferenceType;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.dataelement.DataTemplateElement;

import java.util.Set;

/**
 * DTO for {@link DataTemplateElement}
 */
@AllArgsConstructor
@Getter
@Setter
public class DataElementDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final String code;
    private final String name;
    private final String shortName;
    @NotNull
    private final ValueType type;
    @Size(max = 2000)
    private final String description;
    private final OptionSetDto optionSet;
    private final ReferenceType resourceType;
    private final Set<DataElementGroupDto> dataElementGroups;
}
