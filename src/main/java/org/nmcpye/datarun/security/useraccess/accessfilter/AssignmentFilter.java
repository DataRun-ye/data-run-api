package org.nmcpye.datarun.security.useraccess.accessfilter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.domain.Activity;
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
public class AssignmentFilter extends DefaultJpaFilter<Assignment> {
    public static Specification<Assignment> isEnabled() {
        return (root, query, cb) -> {
            Join<Assignment, Activity> activityJoin = root.join("activity", JoinType.LEFT);
            Join<Assignment, Team> teamJoin = root.join("team", JoinType.LEFT);

            Predicate activityNotDisabled = cb.isFalse(activityJoin.get("disabled"));
            Predicate teamNotDisabled = cb.isFalse(teamJoin.get("disabled"));

            return cb.and(teamNotDisabled, activityNotDisabled);
        };
    }

    @Override
    public Specification<Assignment> getAccessSpecification(CurrentUserDetails user,
                                                            QueryRequest queryRequest) {
        Specification<Assignment> spec = (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            }

            if (user.getTeams() == null || user.getTeams().isEmpty()) {
                return cb.disjunction(); // user has no access
            }

            Join<Assignment, Team> assignmentJoin = root.join("team", JoinType.INNER);
            return assignmentJoin.get("uid").in(user.getTeams());

        };

        if (!queryRequest.isIncludeDisabled()) {
            spec = Specification.where(spec).and(isEnabled());
        }
        return spec;
    }
}
