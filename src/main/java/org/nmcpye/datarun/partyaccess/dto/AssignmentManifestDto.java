package org.nmcpye.datarun.partyaccess.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/// The "Manifest" DTO: Everything the client needs to initialize a context.
///
/// @author Hamza Assada 28/12/2025
@Data
@Builder
public class AssignmentManifestDto {
    private String assignmentUid;
    private String label;
    private List<String> vocabularyUids;
    private List<BindingDto> bindings;

    @Data
    public static class BindingDto {
        /// `Null` if global for assignment
        private String vocabularyUid;
        private String roleName;
        private String partySetUid;
    }
}
