package org.nmcpye.datarun.web.rest.v1.party;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.party.service.ManifestService;
import org.nmcpye.datarun.party.dto.AssignmentManifestDto;
import org.nmcpye.datarun.party.resolution.PartyResolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/context")
@RequiredArgsConstructor
public class ContextController {

    private final ManifestService manifestService;
    private final PartyResolutionService resolutionService;

    /**
     * The Bootstrap Endpoint: Called when the app launches or syncs.
     */
    @GetMapping("/manifest")
    public ResponseEntity<List<AssignmentManifestDto>> getManifest(
        @RequestHeader("X-User-Uid") String userUid, // Or get from SecurityContext
        @RequestParam(required = false) List<String> teamUids
    ) {
        // Validation logic for userUid would go here
        return ResponseEntity.ok(manifestService.buildManifest(userUid, teamUids));
    }

//    /**
//     * The Interactive Endpoint: Called when user taps a dropdown.
//     */
//    @GetMapping("/parties")
//    public ResponseEntity<List<ResolvedParty>> resolveParties(
//        @RequestParam String assignmentUid,
//        @RequestParam String role,
//        @RequestParam(required = false) String vocabularyUid,
//        @RequestParam(required = false) String q // Search query
//    ) {
//        // 1. Resolve UIDs to internal IDs (omitted for brevity, usually done in Service)
//        // String assignId = idService.resolve(assignmentUid); ...
//
//        List<ResolvedParty> matches = resolutionService.resolveParties(assignmentUid, vocabularyUid, role, q);
//        return ResponseEntity.ok(matches);
//    }
}
