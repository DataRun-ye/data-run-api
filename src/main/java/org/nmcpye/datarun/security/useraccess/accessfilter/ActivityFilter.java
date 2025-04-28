package org.nmcpye.datarun.security.useraccess.accessfilter;

import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada, 21/03/2025
 */
@Component
public class ActivityFilter extends DefaultJpaFilter<Activity> {
    public static Specification<Activity> isEnabled() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("disabled"));
    }

    @Override
    public Specification<Activity> getAccessSpecification(CurrentUserDetails user,
                                                          QueryRequest queryRequest) {

        Specification<Activity> spec = (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            }

            if (user.getActivityUIDs() == null || user.getActivityUIDs().isEmpty()) {
                return cb.disjunction(); // user has no access
            }
//                Join<Activity, Assignment> assignmentJoin = root.join("assignments", JoinType.INNER);
//                Join<Assignment, Team> teamJoin = assignmentJoin.join("team", JoinType.INNER);
//                Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);
//
//                return criteriaBuilder.equal(userJoin.get("login"), user.getUsername());
            return root.get("uid").in(user.getActivityUIDs());

        };

        if (!queryRequest.isIncludeDisabled()) {
            spec = Specification.where(spec).and(isEnabled());
        }
        return spec;
    }
}
