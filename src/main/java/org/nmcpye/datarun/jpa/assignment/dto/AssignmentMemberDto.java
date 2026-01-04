package org.nmcpye.datarun.jpa.assignment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignmentMemberDto {
    private Long id;
    private String assignmentId;
    private String memberType; // USER | TEAM | USER_GROUP
    private String memberId;
    private String role; // Optional role for the member within the assignment
}
