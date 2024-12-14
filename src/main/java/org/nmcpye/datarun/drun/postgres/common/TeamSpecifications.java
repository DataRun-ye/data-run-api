package org.nmcpye.datarun.drun.postgres.common;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.IdentifiableRelationalRepository;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

public abstract class TeamSpecifications extends DefaultIdentifiableSpecifications<Team> {

    public TeamSpecifications(IdentifiableRelationalRepository<Team> repository) {
        super(repository);
    }

    public Specification<Team> hasLevel(Integer level) {
        return (root, query, criteriaBuilder) -> level == null ? null : criteriaBuilder.equal(root.get("level"), level);
    }

    public Specification<Team> hasParent(String parent) {
        return (root, query, criteriaBuilder) -> parent == null ? null : criteriaBuilder.equal(root.get("parent"), parent);
    }

    public Specification<Team> isNotDisabled() {
        return (root, query, criteriaBuilder) -> {
            Join<Team, Activity> activityJoin = root.join("activity", JoinType.LEFT);

            Predicate teamNotDisabled = criteriaBuilder.isFalse(root.get("disabled"));
            Predicate activityNotDisabled = criteriaBuilder.isFalse(activityJoin.get("disabled"));

            return criteriaBuilder.and(teamNotDisabled, activityNotDisabled);
        };
    }

    public Specification<Team> canRead(String login) {
        return (root, query, criteriaBuilder) -> {
            if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
                // Admin users can read all teams
                return criteriaBuilder.conjunction();
            } else if (!SecurityUtils.isAuthenticated()) {
                // Unauthenticated users can't read any teams
                return criteriaBuilder.disjunction();
            } else {
                // Regular users can only read teams they're associated with
                Join<Team, User> userJoin = root.join("users", JoinType.INNER);
                return criteriaBuilder.equal(userJoin.get("login"), login);
            }
        };
    }

    // New Specification to check if a specific user is associated with a team
//    public Specification<Team> canRead(String logIn) {
//        if(SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
//            return (root, query, criteriaBuilder) -> criteriaBuilder.disjunction();
//        }
//
//        return (root, query, criteriaBuilder) -> {
//            // Join Team with the users
//            Join<Team, User> userJoin = root.join("users", JoinType.INNER);
//
//            // Check if the userId matches
//            return criteriaBuilder.equal(userJoin.get("login"), logIn);
//        };
//    }
    public Specification<Team> hasAccess() {
        return (root, query, criteriaBuilder) -> {
            if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
                return criteriaBuilder.conjunction();
            } else if (!SecurityUtils.isAuthenticated()) {
                return criteriaBuilder.disjunction();
            } else {
                String currentUserLogin = SecurityUtils.getCurrentUserLogin()
                    .orElseThrow(() -> new IllegalStateException("Current user login not found"));
                Join<Team, User> userJoin = root.join("users", JoinType.INNER);
                return criteriaBuilder.equal(userJoin.get("login"), currentUserLogin);
            }
        };
    }

//    public Specification<Team> hasAccess() {
//        if (SecurityUtils.isAuthenticated()) {
//            return canRead(SecurityUtils.getCurrentUserLogin().get());
//        } else {
//            return (root, query, criteriaBuilder) -> criteriaBuilder.disjunction();
//        }
//    }

//    @Override
//    public Predicate toPredicate(Root<Team> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
//
//        if (criteria.getOperation().equalsIgnoreCase(">")) {
//            return builder.greaterThanOrEqualTo(
//                root.get(criteria.getKey()), criteria.getValue().toString());
//        } else if (criteria.getOperation().equalsIgnoreCase("<")) {
//            return builder.lessThanOrEqualTo(
//                root.get(criteria.getKey()), criteria.getValue().toString());
//        } else if (criteria.getOperation().equalsIgnoreCase(":")) {
//            if (root.get(criteria.getKey()).getJavaType() == String.class) {
//                return builder.like(
//                    root.get(criteria.getKey()), "%" + criteria.getValue() + "%");
//            } else {
//                return builder.equal(root.get(criteria.getKey()), criteria.getValue());
//            }
//        }
//        return null;
//    }
}

