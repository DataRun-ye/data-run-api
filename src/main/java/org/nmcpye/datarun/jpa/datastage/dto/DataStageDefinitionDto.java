package org.nmcpye.datarun.jpa.datastage.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import org.nmcpye.datarun.common.translation.Translation;
import org.nmcpye.datarun.jpa.datastage.DataStageDefinition;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

/**
 * DTO for {@link DataStageDefinition}
 */
@Builder
@Getter
public class DataStageDefinitionDto implements Serializable {
    @Size(max = 11)
    private String uid;
    private String code;
    private String name;
    private String description;
    private Boolean repeatable;
    private Integer stageOrder;
    private String assignmentTypeUid;
    private String dataTemplateUid;
    private String createdBy;
    private Instant createdDate;
    private String lastModifiedBy;
    private Instant lastModifiedDate;
    private Set<Translation> translations;
}
