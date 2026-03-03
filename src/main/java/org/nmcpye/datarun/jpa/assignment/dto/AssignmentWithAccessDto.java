package org.nmcpye.datarun.jpa.assignment.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.enumeration.FlowStatus;

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
public class AssignmentWithAccessDto implements Serializable {
    @Size(max = 26)
    protected String id;
    protected String code;
    String activity;
    String team;
    String orgUnit;
    FlowStatus progressStatus;
    Set<AssignmentFormDto> accessibleForms;
}
