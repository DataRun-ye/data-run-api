package org.nmcpye.datarun.jpa.accessfilter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada
 * @since 21/03/2025
 */
@Component
public class AssignmentFilter extends DefaultJpaFilter<Assignment> {
    private final ManagedAssignmentScope managedAssignmentScope;

    public AssignmentFilter(ManagedAssignmentScope managedAssignmentScope) {
        this.managedAssignmentScope = managedAssignmentScope;
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
    public Specification<Assignment> getAccessSpecification(CurrentUserDetails user,
                                                              QueryRequest queryRequest) {
        final boolean includeDisabled = queryRequest != null && queryRequest.isIncludeDisabled();
        final Set<String> managedAssignmentUids = user.isSuper()
            ? Set.of()
            : managedAssignmentScope.findAccessible(user, includeDisabled).stream()
                .map(Assignment::getUid)
                .collect(Collectors.toSet());

        Specification<Assignment> spec = (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            }

            if (user.getUserTeamsUIDs() == null || user.getUserTeamsUIDs().isEmpty()) {
                return cb.disjunction(); // user has no access
            }

            Join<Assignment, Team> assignmentJoin = root.join("team", JoinType.INNER);
            Predicate ownTeams = assignmentJoin.get("uid").in(user.getUserTeamsUIDs());
            if (managedAssignmentUids.isEmpty()) {
                return ownTeams;
            }
            return cb.or(ownTeams, root.get("uid").in(managedAssignmentUids));
        };

        if (queryRequest == null || !queryRequest.isIncludeDisabled()) {
            spec = spec.and(isEnabled());
        }
        return spec;
    }
}
