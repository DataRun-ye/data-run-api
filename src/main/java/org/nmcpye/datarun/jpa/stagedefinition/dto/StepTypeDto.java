package org.nmcpye.datarun.jpa.stagedefinition.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.nmcpye.datarun.jpa.stagedefinition.StageDefinition;

/**
 * DTO for {@link StageDefinition}
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StepTypeDto extends BaseDto {
    @NotNull
    private String name;
    private String description;
    private Boolean repeatable = false;
    @NotNull
    private Integer stepOrder;
    @NotNull
    private String flowTypeId;
    private FlowType.PlanningMode flowTypePlanningMode;
    private Boolean flowTypeForceStepOrder = false;
    @NotNull
    private String dataTemplateId;
    private String entityBoundTypeId;
}
