package org.nmcpye.datarun.useraccess.accessfilter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * @author Hamza, 21/03/2025
 */
@Component
public class AssignmentFilter extends DefaultJpaFilter<Assignment> {
    public AssignmentFilter(Class<Assignment> clazz) {
        super(clazz);
    }

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
    public Specification<Assignment> createSpecification(CurrentUserDetails user, boolean includeDisabled) {
        Specification<Assignment> spec = (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            } else {
                Join<Assignment, Team> assignmentJoin = root.join("team", JoinType.INNER);
                Join<Team, User> userJoin = assignmentJoin.join("users", JoinType.INNER);
                return cb.equal(userJoin.get("login"), user.getUsername());
            }
        };

        if (includeDisabled) {
            spec = Specification.where(spec).and(isEnabled());
        }
        return spec;
    }
}
