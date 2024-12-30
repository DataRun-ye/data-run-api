package org.nmcpye.datarun.drun.postgres.common;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public abstract class OrgUnitSpecification {

    public static Specification<OrgUnit> hasLevel(Integer level) {
        return (root, query, criteriaBuilder) -> level == null ? null : criteriaBuilder.equal(root.get("level"), level);
    }

    public static Specification<OrgUnit> hasParent(String parent) {
        return (root, query, criteriaBuilder) -> parent == null ? null : criteriaBuilder.equal(root.get("parent"), parent);
    }

    public static Specification<OrgUnit> selectedUnitsByUids(List<String> uids) {
        return (root, query, cb) -> root.get("uid").in(uids);
    }

    public static Specification<OrgUnit> canRead() {
        return (root, query, criteriaBuilder) -> {
            if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
                return criteriaBuilder.conjunction();
            } else if (!SecurityUtils.isAuthenticated()) {
                return null;
            } else {
                if (Long.class != query.getResultType()) {
                    root.fetch("parent", JoinType.LEFT);
                }
                Join<OrgUnit, Assignment> assignmentJoin = root.join("assignments", JoinType.INNER);
                Join<Assignment, Activity> assignmentActivityJoin = assignmentJoin.join("activity", JoinType.INNER);
                Join<Assignment, Team> teamJoin = assignmentJoin.join("team", JoinType.INNER);
                Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);

                Predicate teamNotDisabled = criteriaBuilder.isFalse(teamJoin.get("disabled"));
                Predicate activityNotDisabled = criteriaBuilder.isFalse(assignmentActivityJoin.get("disabled"));

                String currentUserLogin = SecurityUtils.getCurrentUserLogin()
                    .orElseThrow(() -> new IllegalStateException("Current user login not found"));
                query.distinct(true);
                return criteriaBuilder.and(criteriaBuilder.equal(userJoin.get("login"), currentUserLogin),
                    activityNotDisabled,
                    teamNotDisabled);
            }
        };
    }

//    public Specification<OrgUnit> canRead(String login) {
//        return (root, query, criteriaBuilder) -> {
//            Join<OrgUnit, Assignment> assignmentsJoin = root.join("assignments", JoinType.INNER);
//            Join<Assignment, Team> teamJoin = assignmentsJoin.join("team", JoinType.INNER);
//            Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);
//
//            return criteriaBuilder.equal(userJoin.get("login"), login);
//        };
//    }


//    public Specification<OrgUnit> canReadWithAncestors(String login) {
//        return (root, query, criteriaBuilder) -> {
//            // Subquery to fetch distinct paths of readable OrgUnits
//            Subquery<String> readableOrgUnitPaths = query.subquery(String.class);
//            Root<OrgUnit> subRoot = readableOrgUnitPaths.from(OrgUnit.class);
//
//            // Joins for assignments -> team -> users
//            Join<OrgUnit, Assignment> assignmentJoin = subRoot.join("assignments", JoinType.INNER);
//            Join<Assignment, Team> teamJoin = assignmentJoin.join("team", JoinType.INNER);
//            Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);
//
//            // Fetch distinct paths of readable OrgUnits
//            readableOrgUnitPaths.select(subRoot.get("path"))
//                .where(criteriaBuilder.equal(userJoin.get("login"), login));
//
//            // Main query: match direct OrgUnits and their ancestors
//            return criteriaBuilder.or(
//                root.get("path").in(readableOrgUnitPaths), // Direct readable OrgUnits
//                criteriaBuilder.exists(
//                    query.subquery(String.class)
//                        .select(criteriaBuilder.literal(1)) // Check existence of ancestor relationship
//                        .where(
//                            criteriaBuilder.and(
//                                criteriaBuilder.like(root.get("path"), criteriaBuilder.concat(subRoot.get("path"), "%")),
//                                subRoot.get("path").in(readableOrgUnitPaths) // Ensure ancestor paths are unique
//                            )
//                        )
//                )
//            );
//        };
//    }


//    public Specification<OrgUnit> canReadWithAncestors(String login) {
//        return (root, query, criteriaBuilder) -> {
//            // Subquery to fetch orgUnit paths the user can read
//            Subquery<String> readableOrgUnitPaths = query.subquery(String.class);
//            Root<OrgUnit> subRoot = readableOrgUnitPaths.from(OrgUnit.class);
//
//            // Joins for assignments -> team -> users
//            Join<OrgUnit, Assignment> assignmentJoin = subRoot.join("assignments", JoinType.INNER);
//            Join<Assignment, Team> teamJoin = assignmentJoin.join("team", JoinType.INNER);
//            Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);
//
//            // Fetch distinct paths of readable OrgUnits
//            readableOrgUnitPaths.select(subRoot.get("path")).distinct(true)
//                .where(criteriaBuilder.equal(userJoin.get("login"), login));
//
//            // Main query: include ancestors explicitly
//            return criteriaBuilder.or(
//                root.get("path").in(readableOrgUnitPaths), // OrgUnits readable by the user
//                criteriaBuilder.exists(
//                    query.subquery(String.class)
//                        .select(subRoot.get("path"))
//                        .where(criteriaBuilder.like(root.get("path"), subRoot.get("path") + "%"))
//                )
//            );
//        };
//    }

//    public Specification<OrgUnit> canRead(String login) {
//        return (root, query, criteriaBuilder) -> {
//            // Subquery to fetch orgUnit paths the user can read
//            Subquery<String> readableOrgUnitPaths = query.subquery(String.class);
//            Root<OrgUnit> subRoot = readableOrgUnitPaths.from(OrgUnit.class);
//
//            // Joins for assignments -> team -> users
//            Join<OrgUnit, Assignment> assignmentJoin = subRoot.join("assignments", JoinType.INNER);
//            Join<Assignment, Team> teamJoin = assignmentJoin.join("team", JoinType.INNER);
//            Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);
//
//            // Fetch paths of readable OrgUnits
//            readableOrgUnitPaths.select(subRoot.get("path"))
//                .where(criteriaBuilder.equal(userJoin.get("login"), login));
//
//            // Main query: include ancestors
//            return criteriaBuilder.or(
//                root.get("path").in(readableOrgUnitPaths), // OrgUnits readable by the user
//                readableOrgUnitPaths.distinct(true).getSelection().in(root.get("path")) // Ancestors of readable OrgUnits
//            );
//        };
//    }


//    public Specification<OrgUnit> canReadWithAncestors(String login) {
//        return (root, query, criteriaBuilder) -> {
//            // Subquery for organizational units the user can read
//            Subquery<String> orgUnitsSubquery = query.subquery(String.class);
//            Root<OrgUnit> orgUnitsRoot = orgUnitsSubquery.from(OrgUnit.class);
//
//            // Joins for assignments, teams, and users
//            Join<OrgUnit, Assignment> assignmentsJoin = orgUnitsRoot.join("assignments", JoinType.INNER);
//            Join<Assignment, Team> teamJoin = assignmentsJoin.join("team", JoinType.INNER);
//            Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);
//
//            // Subquery selection
//            orgUnitsSubquery.select(orgUnitsRoot.get("path"))
//                .where(criteriaBuilder.equal(userJoin.get("login"), login));
//
//            // Main query to include ancestors based on path
//            return criteriaBuilder.or(
//                criteriaBuilder.equal(userJoin.get("login"), login), // Direct match
//                criteriaBuilder.like(root.get("path"), criteriaBuilder.concat(orgUnitsSubquery, "%")) // Ancestors
//            );
//        };
//    }

//    public Specification<OrgUnit> canRead(String login) {
//        return (root, query, criteriaBuilder) -> {
////            Join<OrgUnit, Assignment> assignmentsJoin = root.join("assignments", JoinType.INNER);
////            Join<Assignment, Team> teamJoin = assignmentsJoin.join("team", JoinType.INNER);
////            Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);
//
//            // Subquery to find OrgUnits directly assigned to the user
//            Subquery<String> subquery = query.subquery(String.class);
//            Root<OrgUnit> subRoot = subquery.from(OrgUnit.class);
//            Join<OrgUnit, Assignment> subAssignmentsJoin = subRoot.join("assignments", JoinType.INNER);
//            Join<Assignment, Team> subTeamJoin = subAssignmentsJoin.join("team", JoinType.INNER);
//            Join<Team, User> subUserJoin = subTeamJoin.join("users", JoinType.INNER);
//            subquery.select(subRoot.get("uid")).where(criteriaBuilder.equal(subUserJoin.get("login"), login));
//
//            // Main query to include ancestors of the OrgUnits found in the subquery
//            return criteriaBuilder.or(
//                root.get("uid").in(subquery),  // OrgUnits directly assigned to the user
//                criteriaBuilder.like(root.get("path"), criteriaBuilder.concat("%,", criteriaBuilder.concat(criteriaBuilder.literal(","), root.get("uid"))))  // Ancestors
//            );
//        };
//    }

}
