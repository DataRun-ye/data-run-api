//package org.nmcpye.datarun.web.rest.v1.party;
//
//import lombok.RequiredArgsConstructor;
//import org.nmcpye.datarun.jpa.assignment.dto.AssignmentManifestDto;
//import org.nmcpye.datarun.party.dto.PagedRequest;
//import org.nmcpye.datarun.party.resolution.PartyResolutionService;
//import org.nmcpye.datarun.party.service.ManifestService;
//import org.nmcpye.datarun.security.CurrentUserDetails;
//import org.nmcpye.datarun.web.rest.common.PagedResponse;
//import org.nmcpye.datarun.web.rest.v1.paging.PagingConfigurator;
//import org.springframework.data.domain.Page;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/v1/context")
//@RequiredArgsConstructor
//public class ContextController {
//
//    private final ManifestService manifestService;
//    private final PartyResolutionService resolutionService;
//
//    /**
//     * The Bootstrap Endpoint: Called when the app launches or syncs.
//     */
//    @GetMapping("/manifest")
//    public ResponseEntity<PagedResponse<AssignmentManifestDto>> getManifest(
//        PagedRequest pagedRequest,
//        @AuthenticationPrincipal CurrentUserDetails user) {
//        // Validation logic for userUid would go here
//        try {
//            Page<AssignmentManifestDto> processedPage = manifestService.buildManifest(user.getId(), user.getUserTeamsIds(),
//                user.getUserGroupsIds(), pagedRequest);
//
//            String next = PagingConfigurator.createNextPageLink(processedPage);
//
//            PagedResponse<AssignmentManifestDto> response =
//                PagingConfigurator.initPageResponse(processedPage, next, "context");
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
////
////    /**
////     * @AuthenticationPrincipal CurrentUserDetails user
////     * The Bootstrap Endpoint: Called when the app launches or syncs.
////     */
////    @GetMapping("/manifest")
////    public ResponseEntity<List<AssignmentManifestDto>> getManifest(
////        @RequestHeader("X-User-Uid") String userUid, // Or get from SecurityContext
////        @RequestParam(required = false) List<String> teamUids
////    ) {
////        // Validation logic for userUid would go here
////        return ResponseEntity.ok(manifestService.buildManifest(userUid, teamUids));
////    }
//
//}
