package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.jpa.assignmenttype.AssignmentType;

import java.io.Serializable;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.assignmenttype.AssignmentType}
 */
@AllArgsConstructor
@Getter
@Setter
public class AssignmentTypeDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final String code;
    private final String name;
    private final AssignmentType.PlanningMode planningMode;
    private final AssignmentType.SubmissionMode submissionMode;
    private final ActivityDto activity;
}
