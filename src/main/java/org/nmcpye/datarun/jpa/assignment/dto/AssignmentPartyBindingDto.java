package org.nmcpye.datarun.jpa.assignment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.nmcpye.datarun.party.dto.CombineMode;

import java.util.UUID;

@Data
@Builder
public class AssignmentPartyBindingDto {
    private UUID id;

    private String uid;

    /// This is the role name, e.g., "sender", "receiver"
    private String name;

    @NotNull
    private String assignmentUid;

    /// The ID of the DataTemplate (can be null for assignment-global roles)
    private String vocabularyUid;

    @NotNull
    private String partySetUid;

    /// Optional: USER, TEAM, USER_GROUP
    private String principalType;

    /// Optional: The ID of the specific user, team, or group
    private String principalUid;

    private CombineMode combineMode;
}
