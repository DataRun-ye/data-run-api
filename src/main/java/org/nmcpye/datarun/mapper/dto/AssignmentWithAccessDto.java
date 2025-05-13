package org.nmcpye.datarun.mapper.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * @author Hamza Assada, 24/04/2025
 */
@Builder
@Getter
@Setter
public class AssignmentWithAccessDto {
    String activity;
    String assignment;
    String team;
    String orgUnit;
    Set<AssignmentFormDto> accessibleForms;
}
