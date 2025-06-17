package org.nmcpye.datarun.jpa.dataelement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;
import org.nmcpye.datarun.datatemplateelement.enumeration.ReferenceType;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.dataelement.DataTemplateElement;

/**
 * DTO for {@link DataTemplateElement}
 *
 * @author Hamza Assada 10/06/2024 <7amza.it@gmail.com>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DataElementDto extends BaseDto {
    private String name;
    private String shortName;
    @NotNull
    private ValueType type;
    @Size(max = 2000)
    private String description;
    private BaseDto optionSet;
    private ReferenceType resourceType;
}
