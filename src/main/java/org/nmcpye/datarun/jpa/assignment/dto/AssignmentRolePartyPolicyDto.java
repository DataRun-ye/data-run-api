package org.nmcpye.datarun.jpa.assignment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.nmcpye.datarun.party.dto.CombineMode;

@Data
@Builder
public class AssignmentRolePartyPolicyDto {
    private String id;

    private String uid;

    private String name;

    @NotNull
    private String assignmentUid;

    /// The ID of the DataTemplate (can be null for assignment-global roles)
    private String vocabularyUid;

    @NotNull
    private String partySetUid;

    private String role;

    private CombineMode combineMode;
}
