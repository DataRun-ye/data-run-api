package org.nmcpye.datarun.acl;

import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.security.CreatUserDetailService;
import org.nmcpye.datarun.userdetail.UserFormAccess;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Hamza Assada 17/05/2025 <7amza.it@gmail.com>
 */
@Component
public class AclBootstrap /*implements CommandLineRunner*/ {

    private final AclService aclService;
    private final AssignmentRepository assignments;
    private final CreatUserDetailService creatUserDetailService;

    public AclBootstrap(AclService aclService, AssignmentRepository assignments, CreatUserDetailService creatUserDetailService) {
        this.aclService = aclService;
        this.assignments = assignments;
        this.creatUserDetailService = creatUserDetailService;
    }


    //    @Override
    public void run(String... args) {
        Authentication sys =
            new UsernamePasswordAuthenticationToken("system", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(sys);

        for (Assignment assignment : assignments.findAll()) {
            // grant team-level metadata READ
            Sid teamSid = new GrantedAuthoritySid("ROLE_TEAM_" + assignment.getTeam().getUid());
            aclService.assignPermissions(assignment, teamSid, MetaPermission.READ);

            // for each user in that team who can submit:
            for (UserFormAccess fa : getFormAccessForAssignment(assignment)) {
                if (fa.canAddSubmission()) {
                    Sid userSid = new PrincipalSid(fa.getUser());
                    aclService.assignPermissions(
                        assignment, userSid, DataPermission.DATA_WRITE);
                }
            }
        }
    }

    Set<UserFormAccess> getFormAccessForAssignment(Assignment assignment) {
        Set<User> users = assignment.getTeam().getUsers();
        Set<UserFormAccess> accesses = new HashSet<>();
        for (User user : users) {
            final var userWithAccess = creatUserDetailService.createUserDetails(user);
            accesses.addAll(userWithAccess.getFormAccess());
        }
        return accesses;
    }
}

