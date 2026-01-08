package org.nmcpye.datarun.jpa.assignment;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/// Control which `data_template` (vocabulary) a *principal* (user/team/group or a `member role`) can see/use inside
/// a specific assignment. e.g. preventing HF users from seeing `Issue` while letting MU officers see it — without hacks.
///
/// @author Hamza Assada 06/01/2026
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AssignmentDataTemplateModel {
    @NotNull
    private UUID id;

    @NotNull
    private String assignmentId;

    /// Precedence Logic: The AssignmentPartyBinding entity correctly allows vocabularyId to be nullable,
    /// enabling the "Global Role" vs "Specific Form Role" logic.
    /// Adaptability: We are using vocabularyId in the DB, but we use DataTemplate in our Services/Mappers
    /// to keep it consistent with our existing code.
    @NotNull
    private String dataTemplateId;

    /// optional USER, USER_GROUP, TEAM
    private String principalType;

    /// optional for specific access control
    private String principalId;

    private String principalRole;

    protected String createdBy;

    protected String lastModifiedBy;

    protected Instant createdDate;
    protected Instant lastModifiedDate;
}
