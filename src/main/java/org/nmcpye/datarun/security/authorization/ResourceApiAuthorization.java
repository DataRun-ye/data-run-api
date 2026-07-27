package org.nmcpye.datarun.security.authorization;

import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.stereotype.Service;

/**
 * Coarse authorization for the inherited resource API surface.
 *
 * <p>Entity-specific read filtering remains owned by the access-filter services.
 */
@Service
public class ResourceApiAuthorization {

    public boolean canRead(CurrentUserDetails user) {
        return user != null
            && (user.isSuper()
                || (user.getUserTeamsUIDs() != null && !user.getUserTeamsUIDs().isEmpty()));
    }

    public boolean canManage(CurrentUserDetails user) {
        return user != null && user.isSuper();
    }
}
