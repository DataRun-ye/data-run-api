package org.nmcpye.datarun.drun.common;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.springframework.data.jpa.domain.Specification;

public class AssignmentSpecifications {

    public static Specification<Assignment> hasUserWithUsername(String login) {
        return (root, query, criteriaBuilder) -> {
            // Join assignment -> team -> users
            Join<Assignment, Team> teamJoin = root.join("team", JoinType.INNER);
            Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);

            // Create the predicate for the username
            return criteriaBuilder.equal(userJoin.get("login"), login);
        };
    }
}
