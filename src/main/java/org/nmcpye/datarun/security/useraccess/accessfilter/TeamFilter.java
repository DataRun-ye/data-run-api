package org.nmcpye.datarun.security.useraccess.accessfilter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada, 21/03/2025
 */
@Component
public class TeamFilter extends DefaultJpaFilter<Team> {
    public static Specification<Team> isEnabled() {
        return (root, query, criteriaBuilder) -> {
            Join<Team, Activity> activityJoin = root.join("activity", JoinType.LEFT);

            Predicate teamNotDisabled = criteriaBuilder.isFalse(root.get("disabled"));
            Predicate activityNotDisabled = criteriaBuilder.isFalse(activityJoin.get("disabled"));

            return criteriaBuilder.and(teamNotDisabled, activityNotDisabled);
        };
    }

    @Override
    public Specification<Team> getAccessSpecification(CurrentUserDetails user,
                                                      QueryRequest queryRequest) {
        Specification<Team> spec = (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            }

            if (user.getUserTeamIds() == null || user.getUserTeamIds().isEmpty()) {
                return cb.disjunction(); // user has no access
            }

            return root.get("uid").in(user.getUserTeamIds());
//            Join<Team, User> userJoin = root.join("users", JoinType.INNER);
//            return cb.equal(userJoin.get("login"), user.getUsername());

        };

        if (!queryRequest.isIncludeDisabled()) {
            spec = Specification.where(spec).and(isEnabled());
        }
        return spec;
    }
}
