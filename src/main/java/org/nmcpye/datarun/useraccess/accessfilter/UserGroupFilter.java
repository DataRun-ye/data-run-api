package org.nmcpye.datarun.useraccess.accessfilter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.UserGroup;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * @author Hamza, 21/03/2025
 */
@Component
public class UserGroupFilter extends DefaultJpaFilter<UserGroup> {
    public UserGroupFilter(Class<UserGroup> clazz) {
        super(clazz);
    }

    public static Specification<UserGroup> isEnabled() {
        return (root, query, criteriaBuilder) -> {

            Predicate userGroupNotDisabled = criteriaBuilder.isFalse(root.get("disabled"));

            return criteriaBuilder.and(userGroupNotDisabled);
        };
    }

    @Override
    public Specification<UserGroup> createSpecification(CurrentUserDetails user, boolean includeDisabled) {
        Specification<UserGroup> spec = (root, query, criteriaBuilder) -> {
            if (user.isSuper()) {
                return criteriaBuilder.conjunction();
            } else {
                Join<UserGroup, User> userJoin = root.join("users", JoinType.INNER);
                return criteriaBuilder.equal(userJoin.get("login"), user.getUsername());
            }
        };

        if (includeDisabled) {
            spec = Specification.where(spec).and(isEnabled());
        }
        return spec;
    }
}
