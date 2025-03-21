package org.nmcpye.datarun.useraccess.accessfilter;

import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * @author Hamza, 21/03/2025
 */
@Component
public class UserFilter extends DefaultJpaFilter<User> {
    public UserFilter(Class<User> clazz) {
        super(clazz);
    }

    @Override
    public Specification<User> createSpecification(CurrentUserDetails user, boolean includeDisabled) {
        return (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            } else {

                return cb.equal(root.get("login"), user.getUsername());
            }
        };
    }
}
