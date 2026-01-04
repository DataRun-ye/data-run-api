package org.nmcpye.datarun.jpa.assignment.dto;

import lombok.Builder;
import lombok.Data;
import org.nmcpye.datarun.party.dto.CombineMode;

import java.util.UUID;

@Data
@Builder
public class AssignmentPartyBindingDtoOld {
    private UUID id;
    private String uid;

    /// This is the role name, e.g., "sender", "receiver"
    private String name;

    private String assignmentId;
    private String assignmentUid;

    /// The ID of the DataTemplate (can be null for assignment-global roles)
    private String vocabularyId;
    private String vocabularyUid;

    private UUID partySetId;
    private String partySetUid;

    /// Optional: USER, TEAM, USER_GROUP
    private String principalType;

    /// Optional: The ID of the specific user, team, or group
    private String principalId;
    private String principalUid;

    private CombineMode combineMode;
}
