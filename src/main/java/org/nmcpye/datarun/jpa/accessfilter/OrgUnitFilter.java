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
    private final AssignmentRepository assignmentRepository;

    public OrgUnitFilter(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public Specification<OrgUnit> getAccessSpecification(CurrentUserDetails user,
                                                         QueryRequest queryRequest) {
        final boolean includeDisabled =
            queryRequest != null && queryRequest.isIncludeDisabled();
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

        return (root, query, cb) -> user.isSuper()
            ? cb.conjunction()
            : root.get("uid").in(allUids);
    }

    Set<OrgUnit> getDirectOrgUnits(
        CurrentUserDetails user,
        boolean includeDisabled
    ) {
        return assignmentRepository
            .findAllByTeamUidIn(user.getUserTeamsUIDs())
            .stream()
            .filter(assignment ->
                !Boolean.TRUE.equals(assignment.getDeleted())
            )
            .filter(assignment -> includeDisabled
                || (!Boolean.TRUE.equals(assignment.getTeam().getDisabled())
                && !Boolean.TRUE.equals(
                    assignment.getActivity().getDisabled()
                )))
            .map(Assignment::getOrgUnit)
            .collect(Collectors.toSet());
    }
}
