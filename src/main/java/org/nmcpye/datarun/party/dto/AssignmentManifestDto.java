package org.nmcpye.datarun.party.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/// The "Manifest" DTO: Everything the client needs to initialize a context.
///
/// @author Hamza Assada 28/12/2025
@Data
@Builder(toBuilder = true)
public class AssignmentManifestDto {
    private String assignmentUid;
    private String label;
    private AssignmentStatus status; // IN_PROGRESS, DONE, EXPIRED, CANCELLED
    private List<String> templateUids;
    private List<BindingDto> bindings;

    @Data
    @Builder
    public static class BindingDto {
        /// `Null` if global for assignment
        private String templateUid;
        private String roleName;
        private UUID partySetId;

        CombineMode combineMode;
        /**
         * For debugging/audit: "Role Binding (Team X)", "Assignment Default", etc.
         */
        String provenance;
    }
}
