package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.jpa.flowtype.FlowType;

/**
 * DTO for {@link FlowType}
 */
@AllArgsConstructor
@Getter
@Setter
public class AssignmentTypeDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final String code;
    private final String name;
    private final FlowType.PlanningMode planningMode;
    private final FlowType.SubmissionMode submissionMode;
    private final ActivityDto activity;
}
