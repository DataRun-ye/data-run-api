package org.nmcpye.datarun.useraccess.accessfilter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * @author Hamza, 21/03/2025
 */
@Component
public class TeamFilter extends DefaultJpaFilter<Team> {
    public TeamFilter(Class<Team> clazz) {
        super(clazz);
    }

    public static Specification<Team> isEnabled() {
        return (root, query, criteriaBuilder) -> {
            Join<Team, Activity> activityJoin = root.join("activity", JoinType.LEFT);

            Predicate teamNotDisabled = criteriaBuilder.isFalse(root.get("disabled"));
            Predicate activityNotDisabled = criteriaBuilder.isFalse(activityJoin.get("disabled"));

            return criteriaBuilder.and(teamNotDisabled, activityNotDisabled);
        };
    }

    @Override
    public Specification<Team> createSpecification(CurrentUserDetails user, boolean includeDisabled) {
        Specification<Team> spec = (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            } else {
                Join<Team, User> userJoin = root.join("users", JoinType.INNER);
                return cb.equal(userJoin.get("login"), user.getUsername());
            }
        };

        if (includeDisabled) {
            spec = Specification.where(spec).and(isEnabled());
        }
        return spec;
    }
}
