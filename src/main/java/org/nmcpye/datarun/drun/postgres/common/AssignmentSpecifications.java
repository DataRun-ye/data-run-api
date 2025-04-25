package org.nmcpye.datarun.drun.postgres.common;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public abstract class AssignmentSpecifications {
    /**
     * <pre>{@code
     *  List<OrgUnit> results = orgUnitRepo.findAll(
     *  hasAncestors(accessibleIds, includeDisabled)
     *     .and((root, query, cb) -> cb.equal(root.get("code"), "SALES"))
     * );
     *
     * // Use pagination to process large datasets in chunks:
     * Page<OrgUnit> page = repo.findAll(
     *     hasAncestors(accessibleIds, includeDisabled),
     *     PageRequest.of(0, 400)
     * );
     * }</pre>
     *
     * @param accessibleIds
     * @return specification
     */
    public static Specification<Assignment> hasAncestors(List<Long> accessibleIds) {
        return (root, query, cb) -> {
            // Subquery to get all ancestor UIDs from accessible OrgUnits' paths
            Subquery<String> subquery = query.subquery(String.class);
            Root<Assignment> acc = subquery.from(Assignment.class);
            subquery.select(
                cb.function("unnest", String.class,
                    cb.function("string_to_array", String[].class, acc.get("path"), cb.literal(","))
                )
            ).where(acc.get("id").in(accessibleIds));

            // Match current OrgUnit's UID against the collected ancestor UIDs
            /// isAncestor
            return root.get("uid").in(subquery);
        };
    }

    public static Specification<Assignment> hasDescendants(List<Long> accessibleIds) {
        return (root, query, cb) -> {
            // Subquery to get paths of accessible OrgUnits
            Subquery<String> subquery = query.subquery(String.class);
            Root<Assignment> acc = subquery.from(Assignment.class);
            subquery.select(acc.get("path"))
                .where(acc.get("id").in(accessibleIds));

            // Check if current OrgUnit's path starts with any accessible path
            return cb.or(
                root.get("id").in(accessibleIds), // Include the accessible OrgUnit itself
                cb.exists(subquery.where(
                    cb.like(root.get("path"), cb.concat(acc.get("path"), ",%"))
                ))
            );
        };
    }

    //    @Override
    public static Specification<Assignment> canRead() {
        return (root, query, criteriaBuilder) -> {
            if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
                return criteriaBuilder.conjunction();
            } else if (!SecurityUtils.isAuthenticated()) {
                return criteriaBuilder.disjunction();
            } else {
                String currentUserLogin = SecurityUtils.getCurrentUserLoginOrThrow(
                    new ErrorMessage(ErrorCode.E3004, Assignment.class.getName()));
                Join<Assignment, Team> assignmentJoin = root.join("team", JoinType.INNER);
                Join<Team, User> userJoin = assignmentJoin.join("users", JoinType.INNER);
                return criteriaBuilder.equal(userJoin.get("login"), currentUserLogin);
            }
        };
    }
}
