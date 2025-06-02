package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * DTO for {@link org.nmcpye.datarun.datatemplateelement.DataOption}
 */
@AllArgsConstructor
@Getter
@Setter
public class DataOptionDto extends AbstractBaseDto {
    @NotNull
    private final String listName;
    @NotNull
    private final String name;
    private final Integer order;
    private final Map<String, String> label;
    private final String filterExpression;
}
