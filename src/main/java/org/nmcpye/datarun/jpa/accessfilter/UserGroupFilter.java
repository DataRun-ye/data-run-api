package org.nmcpye.datarun.jpa.accessfilter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.jpa.usegroup.UserGroup;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada, 21/03/2025
 */
@Component
public class UserGroupFilter extends DefaultJpaFilter<UserGroup> {
    public static Specification<UserGroup> isEnabled() {
        return (root, query, criteriaBuilder) -> {

            Predicate userGroupNotDisabled = criteriaBuilder.isFalse(root.get("disabled"));

            return criteriaBuilder.and(userGroupNotDisabled);
        };
    }

    @Override
    public Specification<UserGroup> getAccessSpecification(CurrentUserDetails user,
                                                           QueryRequest queryRequest) {
        Specification<UserGroup> spec = (root, query, criteriaBuilder) -> {
            if (user.isSuper()) {
                return criteriaBuilder.conjunction();
            } else {
                Join<UserGroup, User> userJoin = root.join("users", JoinType.INNER);
                return criteriaBuilder.equal(userJoin.get("login"), user.getUsername());
            }
        };

        if (queryRequest == null || !queryRequest.isIncludeDisabled()) {
            spec = spec.and(isEnabled());
        }
        return spec;
    }
}
