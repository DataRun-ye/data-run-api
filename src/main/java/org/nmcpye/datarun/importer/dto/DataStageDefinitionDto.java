package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.jpa.stagedefinition.StageDefinition;

/**
 * DTO for {@link StageDefinition}
 */
@AllArgsConstructor
@Getter
@Setter
public class DataStageDefinitionDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final Boolean deleted;
    private final String code;
    private final String name;
    private final String description;
    private final Boolean repeatable;
    private final Integer stageOrder;
    private final AssignmentTypeDto assignmentType;
    private final DataTemplateDto dataTemplate;
}
