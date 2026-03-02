package org.nmcpye.datarun.jpa.accessfilter;

import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.query.QueryRequest;
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
        final var directOrgUnits = getDirectAndManagedOrgUnit(user, queryRequest.isIncludeDisabled());
        final var directAndManagedUids = directOrgUnits.stream().map(OrgUnit::getUid).collect(Collectors.toSet());

        final Set<String> ancestorsUids = directOrgUnits
            .stream()
            .flatMap(o -> o.getAncestorUids(null)
                .stream())
            .collect(Collectors.toSet());

        final var allUids = Stream
            .concat(ancestorsUids.stream(), directAndManagedUids.stream())
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

    Set<OrgUnit> getDirectAndManagedOrgUnit(CurrentUserDetails user, boolean includeDisabled) {
        final var orgUnitSet = flowInstanceRepository.findAllByTeamUidIn(
            Stream.concat(user.getUserTeamsUIDs().stream(),
                    user.getManagedTeamsUIDs().stream())
                .collect(Collectors.toSet()));

        return !includeDisabled ? orgUnitSet
            .stream()
            .filter(assignment -> !assignment.getTeam().getDisabled() || !assignment.getActivity().getDisabled())
            .map(Assignment::getOrgUnit)
            .collect(Collectors.toSet()) : orgUnitSet
            .stream()
            .map(Assignment::getOrgUnit)
            .collect(Collectors.toSet());
    }
}
