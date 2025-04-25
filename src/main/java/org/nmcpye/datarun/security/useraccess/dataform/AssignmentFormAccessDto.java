package org.nmcpye.datarun.security.useraccess.dataform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.nmcpye.datarun.common.security.UserFormAccess;

import java.util.List;

/**
 * @author Hamza Assada, 24/04/2025
 */
@Builder
@AllArgsConstructor
public class AssignmentFormAccessDto {
    String assignment;     // Assignment identifier
    String team;           // Team context
    String orgUnit;           // Team context
    String activity;           // Team context
    List<UserFormAccess> forms; // Access info per form
}
