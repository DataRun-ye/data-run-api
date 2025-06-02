package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.datavalue.DataValue}
 */
@AllArgsConstructor
@Getter
@Setter
public class DataValueDto extends AbstractBaseDto {
    @Size(max = 50000)
    private final String value;
    private final Boolean followup;
    private final String comment;
    private final String storedBy;
    private final DataElementDto dataElement;
}
