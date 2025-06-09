package org.nmcpye.datarun.jpa.flowrun.dto;

import lombok.Builder;
import lombok.Getter;
import org.nmcpye.datarun.common.enumeration.FlowRunStatus;

import java.util.Set;

/**
 * @author Hamza Assada 24/04/2025 <7amza.it@gmail.com>
 */
@Builder
@Getter
public class AssignmentWithAccessDto {
    String activity;
    String assignment;
    String team;
    String orgUnit;
    FlowRunStatus progressStatus;
    Set<AssignmentFormDto> accessibleForms;
}
