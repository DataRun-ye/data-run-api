package org.nmcpye.datarun.mapper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.nmcpye.datarun.common.security.UserFormAccess;

import java.util.List;

/**
 * @author Hamza Assada, 24/04/2025
 */
@Builder
@AllArgsConstructor
public class AssignmentWithAccessDto {
    String activity;
    String assignment;
    String team;
    String orgUnit;
    List<UserFormAccess> accessibleForms;
}
