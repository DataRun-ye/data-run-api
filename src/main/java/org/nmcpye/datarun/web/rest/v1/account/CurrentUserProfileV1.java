package org.nmcpye.datarun.web.rest.v1.account;

import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

/**
 * Compatibility adapter for the released {@code /api/v1/myDetails} response.
 *
 * <p>The security principal is deliberately not the HTTP response model.
 * Legacy count and user-group fields remain in this V1 shape only while older
 * supported mobile clients may request them. Retire those fields after the
 * supported mobile baseline no longer depends on the V1 profile shape.</p>
 */
public record CurrentUserProfileV1(
    String id,
    String username,
    String firstName,
    String lastName,
    String mobile,
    String email,
    String langKey,
    boolean activated,
    String imageUrl,
    Collection<? extends GrantedAuthority> authorities,
    Integer assignmentCount,
    Integer orgUnitCount,
    Set<String> activityUIDs,
    Set<String> userTeamsUIDs,
    Set<String> managedTeamsUIDs,
    Set<String> userGroupsUIDs,
    Set<String> userFormsUIDs
) {
    public static CurrentUserProfileV1 from(CurrentUserDetails user) {
        return new CurrentUserProfileV1(
            user.getUid(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getMobile(),
            user.getEmail(),
            user.getLangKey(),
            user.isEnabled(),
            user.getImageUrl(),
            user.getAuthorities(),
            user.getAssignmentCount(),
            user.getOrgUnitCount(),
            user.getActivityUIDs(),
            user.getUserTeamsUIDs(),
            user.getManagedTeamsUIDs(),
            Set.of(),
            user.getUserFormsUIDs()
        );
    }
}
