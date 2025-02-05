package org.nmcpye.datarun.drun.postgres.common;

import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

public abstract class UserSpecifications {

    public static Specification<User> canRead() {
        return (root, query, cb) -> {
            if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
                return cb.conjunction();
            } else if (!SecurityUtils.isAuthenticated()) {
                return cb.disjunction();
            } else {
                String currentUserLogin = SecurityUtils.getCurrentUserLogin()
                    .orElseThrow(() -> new IllegalStateException("Current user login not found"));

                return cb.equal(root.get("login"), currentUserLogin);
            }
        };
    }
}

