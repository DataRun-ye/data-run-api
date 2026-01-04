//package org.nmcpye.datarun.web.rest.v1.party;
//
//import lombok.RequiredArgsConstructor;
//import org.nmcpye.datarun.jpa.assignment.dto.AssignmentMemberDto;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/admin/assignments/{assignmentId}/members")
//@RequiredArgsConstructor
//public class AssignmentMemberResource {
//
//    private final AssignmentMemberService memberService;
//
//    @GetMapping
//    public ResponseEntity<List<AssignmentMemberDto>> getAssignmentMembers(@PathVariable String assignmentId) {
//        return ResponseEntity.ok(memberService.findByAssignmentId(assignmentId));
//    }
//
//    @PostMapping
//    public ResponseEntity<AssignmentMemberDto> addMemberToAssignment(@PathVariable String assignmentId,
//                                                                     @RequestBody AssignmentMemberDto dto) throws URISyntaxException {
//        dto.setAssignmentId(assignmentId);
//        AssignmentMemberDto result = memberService.save(dto);
//        URI location = new URI(String.format("/api/admin/assignments/%s/members/%d", assignmentId, result.getId()));
//        return ResponseEntity.created(location).body(result);
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> removeMemberFromAssignment(@PathVariable String assignmentId, @PathVariable Long id) {
//        memberService.delete(id); // The assignmentId in the path is for context, the member ID is unique
//        return ResponseEntity.noContent().build();
//    }
//}
