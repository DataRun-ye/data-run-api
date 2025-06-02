package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.optionset.OptionSet}
 */
@AllArgsConstructor
@Getter
@Setter
public class OptionSetDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final String code;
    private final String name;
    private final List<DataOptionDto> options;
}
