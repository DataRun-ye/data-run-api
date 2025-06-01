package org.nmcpye.datarun.mapper.dto;

import jakarta.validation.constraints.Size;
import lombok.Value;
import org.nmcpye.datarun.drun.postgres.common.translation.Translation;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

/**
 * DTO for {@link org.nmcpye.datarun.datastage.DataStageDefinition}
 */
@Value
public class DataStageDefinitionDto implements Serializable {
    String createdBy;
    Instant createdDate;
    String lastModifiedBy;
    Instant lastModifiedDate;
    Set<Translation> translations;
    @Size(max = 11)
    String uid;
    String code;
    String name;
    String description;
    Boolean repeatable;
    Integer stageOrder;
    String assignmentTypeUid;
    String dataTemplateUid;
}
