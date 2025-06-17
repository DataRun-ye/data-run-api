package org.nmcpye.datarun.jpa.accessfilter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada 21/03/2025 <7amza.it@gmail.com>
 */
@Component
public class AssignmentFilter extends DefaultJpaFilter<FlowInstance> {
    public static Specification<FlowInstance> isEnabled() {
        return (root, query, cb) -> {
            Join<FlowInstance, Activity> activityJoin = root.join("activity", JoinType.LEFT);
            Join<FlowInstance, Team> teamJoin = root.join("team", JoinType.LEFT);

            Predicate activityNotDisabled = cb.isFalse(activityJoin.get("disabled"));
            Predicate teamNotDisabled = cb.isFalse(teamJoin.get("disabled"));

            return cb.and(teamNotDisabled, activityNotDisabled);
        };
    }

    @Override
    public Specification<FlowInstance> getAccessSpecification(CurrentUserDetails user,
                                                              QueryRequest queryRequest) {
        Specification<FlowInstance> spec = (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            }

            if (user.getUserTeamsUIDs() == null || user.getUserTeamsUIDs().isEmpty()) {
                return cb.disjunction(); // user has no access
            }

            Join<FlowInstance, Team> assignmentJoin = root.join("team", JoinType.INNER);
            return assignmentJoin.get("uid").in(user.getUserTeamsUIDs());

        };

        if (queryRequest == null || !queryRequest.isIncludeDisabled()) {
            spec = spec.and(isEnabled());
        }
        return spec;
    }
}
