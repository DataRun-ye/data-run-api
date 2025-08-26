package org.nmcpye.datarun.jpa.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.common.uidgenerate.BaseDto;

import java.io.Serializable;
import java.util.Set;

/**
 * @author Hamza Assada
 * @since 24/04/2025
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AssignmentWithAccessDto extends BaseDto implements Serializable {
    String activity;
    String team;
    String orgUnit;
    FlowStatus progressStatus;
    Set<AssignmentFormDto> accessibleForms;
}
