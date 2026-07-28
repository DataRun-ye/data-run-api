package org.nmcpye.datarun.jpa.accessfilter;

import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Hamza Assada
 * @since 21/03/2025
 */
@Component
public class OrgUnitFilter extends DefaultJpaFilter<OrgUnit> {
    private final AssignmentRepository flowInstanceRepository;

    public OrgUnitFilter(AssignmentRepository flowInstanceRepository) {
        this.flowInstanceRepository = flowInstanceRepository;
    }

    @Override
    public Specification<OrgUnit> getAccessSpecification(CurrentUserDetails user,
                                                         QueryRequest queryRequest) {
        final boolean includeDisabled = queryRequest != null && queryRequest.isIncludeDisabled();
        final var directOrgUnits = getDirectOrgUnits(user, includeDisabled);
        final var directUids = directOrgUnits.stream().map(OrgUnit::getUid).collect(Collectors.toSet());

        final Set<String> ancestorsUids = directOrgUnits
            .stream()
            .flatMap(o -> o.getAncestorUids(null)
                .stream())
            .collect(Collectors.toSet());

        final var allUids = Stream
            .concat(ancestorsUids.stream(), directUids.stream())
            .collect(Collectors.toSet());

        return (root, query, cb) -> {
            if (user.isSuper()) {
                return cb.conjunction();
            } else {
                return root.get("uid").in(allUids);
//                if (Long.class != query.getResultType()) {
//                    root.fetch("parent", JoinType.LEFT);
//                }
//                Join<OrgUnit, Assignment> assignmentJoin = root.join("assignments", JoinType.INNER);
//                Join<Assignment, Activity> assignmentActivityJoin = assignmentJoin.join("activity", JoinType.INNER);
//                Join<Assignment, Team> teamJoin = assignmentJoin.join("team", JoinType.INNER);
//                Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);
//
//                Predicate teamNotDisabled = includeDisabled ? cb.and() : cb.isFalse(teamJoin.get("disabled"));
//                Predicate activityNotDisabled = includeDisabled ? cb.and() : cb.isFalse(assignmentActivityJoin.get("disabled"));
//
//                query.distinct(true);
//                return cb.and(cb.equal(userJoin.get("login"), user.getUsername()),
//                    activityNotDisabled,
//                    teamNotDisabled);
            }
        };
    }

    Set<OrgUnit> getDirectOrgUnits(CurrentUserDetails user, boolean includeDisabled) {
        final var orgUnitSet = flowInstanceRepository.findAllByTeamUidIn(user.getUserTeamsUIDs());

        return orgUnitSet.stream()
            .filter(assignment -> !Boolean.TRUE.equals(assignment.getDeleted()))
            .filter(assignment -> includeDisabled
                || (!Boolean.TRUE.equals(assignment.getTeam().getDisabled())
                && !Boolean.TRUE.equals(assignment.getActivity().getDisabled())))
            .map(Assignment::getOrgUnit)
            .collect(Collectors.toSet());
    }
}
