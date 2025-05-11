package org.nmcpye.datarun.dto.permissions;

import lombok.Builder;
import lombok.Value;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <10-05-2025>
 */
@Value
@Builder
public class PermissionsDto {
    boolean canViewSubmission;
    boolean canAddSubmission;
    boolean canEditSubmission;
    boolean canApproveSubmission;
    boolean canDeleteSubmission;
}

