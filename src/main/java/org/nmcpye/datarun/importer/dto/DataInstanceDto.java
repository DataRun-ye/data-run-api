package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.jpa.stagesubmission.StageInstance;

import java.time.Instant;

/**
 * DTO for {@link StageInstance}
 */
@AllArgsConstructor
@Getter
@Setter
public class DataInstanceDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final Boolean deleted;
    private final FlowStatus flowStatus;
    private final AssignmentDto assignment;
    private final EntityInstanceDto entityInstance;
    private final String orgUnitUid;
    private final String assignmentTypeUid;
    private final DataStageDefinitionDto dataStageDefinition;
    private final DataTemplateDto dataTemplate;
    private final String dataTemplateVerUid;
    private final Integer dataInstanceVer;
    private final Instant startEntryTime;
    private final Instant finishedEntryTime;
//    private final Set<DataValueDto> dataValues;
}
