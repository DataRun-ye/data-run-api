package org.nmcpye.datarun.drun.common;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.springframework.data.jpa.domain.Specification;

public class TeamSpecifications {

    public static Specification<Team> hasLevel(Integer level) {
        return (root, query, criteriaBuilder) -> level == null ? null : criteriaBuilder.equal(root.get("level"), level);
    }

    public static Specification<Team> hasParent(String parent) {
        return (root, query, criteriaBuilder) -> parent == null ? null : criteriaBuilder.equal(root.get("parent"), parent);
    }

    public static Specification<Team> isNotDisabled() {
        return (root, query, criteriaBuilder) -> {
            Join<Team, Activity> activityJoin = root.join("activity", JoinType.LEFT);

            Predicate teamNotDisabled = criteriaBuilder.isFalse(root.get("disabled"));
            Predicate activityNotDisabled = criteriaBuilder.isFalse(activityJoin.get("disabled"));

            return criteriaBuilder.and(teamNotDisabled, activityNotDisabled);
        };
    }

    // New Specification to check if a specific user is associated with a team
    public static Specification<Team> hasUser(String logIn) {
        return (root, query, criteriaBuilder) -> {
            // Join Team with the users
            Join<Team, User> userJoin = root.join("users", JoinType.INNER);

            // Check if the userId matches
            return criteriaBuilder.equal(userJoin.get("login"), logIn);
        };
    }
}

