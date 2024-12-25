//package org.nmcpye.datarun.web.rest.postgres;
//
//import jakarta.validation.Valid;
//import org.nmcpye.datarun.drun.postgres.domain.TeamFormPermission;
//import org.nmcpye.datarun.drun.postgres.repository.TeamFormPermissionRepository;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/api/formPermissions")
//public class FormPermissionController {
//
//    private final TeamFormPermissionRepository permissionRepository;
//
//    public FormPermissionController(TeamFormPermissionRepository permissionRepository) {
//        this.permissionRepository = permissionRepository;
//    }
//
//    @PostMapping("")
//    public ResponseEntity<String> configureFormPermissions(@RequestBody @Valid List<TeamFormPermission> permissions) {
//        List<TeamFormPermission> updatedPermissions = new ArrayList<>();
//
//        for (TeamFormPermission permission : permissions) {
//            Optional<TeamFormPermission> existingPermission =
//                permissionRepository.findByTeamAndForm(permission.getTeam(), permission.getForm());
//
//            if (existingPermission.isPresent()) {
//                // Update existing permission
//                TeamFormPermission updated = existingPermission.get();
//                updated.setPermissions(permission.getPermissions()); // Update fields as needed
//                permissionRepository.save(updated);
//            } else {
//                // Save as new entry
//                permissionRepository.save(permission);
//            }
//
//            updatedPermissions.add(permission);
//        }
//
//        return ResponseEntity.ok("Permissions successfully configured for the provided form templates.");
//    }
//}
//
