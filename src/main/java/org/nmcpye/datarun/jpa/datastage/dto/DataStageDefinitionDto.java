package org.nmcpye.datarun.jpa.datastage.dto;

import jakarta.validation.constraints.Size;
import lombok.Value;
import org.nmcpye.datarun.common.translation.Translation;
import org.nmcpye.datarun.jpa.datastage.DataStageDefinition;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

/**
 * DTO for {@link DataStageDefinition}
 */
@Value
public class DataStageDefinitionDto implements Serializable {
    @Size(max = 11)
    String uid;
    String code;
    String name;
    String description;
    Boolean repeatable;
    Integer stageOrder;
    String assignmentTypeUid;
    String dataTemplateUid;
    String createdBy;
    Instant createdDate;
    String lastModifiedBy;
    Instant lastModifiedDate;
    Set<Translation> translations;
}
