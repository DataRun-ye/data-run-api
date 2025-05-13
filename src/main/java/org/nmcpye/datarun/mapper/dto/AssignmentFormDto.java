package org.nmcpye.datarun.mapper.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Builder
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AssignmentFormDto implements Serializable {
    private String assignmentUid;
    private String formUid;
    private boolean canViewSubmissions;
    private boolean canAddSubmissions;
    private boolean canEditSubmissions;
    private boolean canApproveSubmissions;
    private boolean canDeleteSubmissions;
}
