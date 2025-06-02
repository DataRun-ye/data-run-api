package org.nmcpye.datarun.importer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.AssignmentStatus;

/**
 * DTO for {@link org.nmcpye.datarun.jpa.assignment.Assignment}
 */
@AllArgsConstructor
@Getter
@Setter
public class AssignmentDto extends AbstractBaseDto {
    @Size(max = 11)
    private final String uid;
    private final ActivityDto activity;
    private final OrgUnitDto orgUnit;
    private final TeamDto team;
    private final AssignmentDto parent;
    private final AssignmentStatus status;
    private final AssignmentTypeDto assignmentType;
    private final String entityInstanceId;
    private final String stageStates;
}
