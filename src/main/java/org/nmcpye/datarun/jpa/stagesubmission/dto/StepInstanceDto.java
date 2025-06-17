package org.nmcpye.datarun.jpa.stagesubmission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;
import org.nmcpye.datarun.jpa.stagesubmission.StageInstance;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for {@link StageInstance}
 */
@Getter
@Setter
@AllArgsConstructor
public class StepInstanceDto extends BaseDto {
    private Boolean deleted;
    private Instant deletedAt;
    private String stepTypeId;
    @NotNull
    private String dataTemplateId;
    @NotNull
    private StageInstance.SubmissionStatus status;
    @NotNull
    private Map<String, Object> data;
    private Instant startEntryTime;
    private Instant finishedEntryTime;
}
