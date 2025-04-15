package org.nmcpye.datarun.useraccess.accessfilter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.Team;
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

        Specification<Activity> spec = (root, query, criteriaBuilder) -> {
            if (user.isSuper()) {
                return criteriaBuilder.conjunction();
            } else {
                Join<Activity, Assignment> assignmentJoin = root.join("assignments", JoinType.INNER);
                Join<Assignment, Team> teamJoin = assignmentJoin.join("team", JoinType.INNER);
                Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);

                return criteriaBuilder.equal(userJoin.get("login"), user.getUsername());
            }
        };

        if (queryRequest.isIncludeDisabled()) {
            spec = Specification.where(spec).and(isEnabled());
        }
        return spec;
    }
}
