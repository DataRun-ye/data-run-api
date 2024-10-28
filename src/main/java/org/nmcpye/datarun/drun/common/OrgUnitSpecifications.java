package org.nmcpye.datarun.drun.common;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.springframework.data.jpa.domain.Specification;

public class OrgUnitSpecifications {
    public static Specification<OrgUnit> hasLevel(Integer level) {
        return (root, query, criteriaBuilder) -> level == null ? null : criteriaBuilder.equal(root.get("level"), level);
    }

    public static Specification<OrgUnit> hasParent(String parent) {
        return (root, query, criteriaBuilder) -> parent == null ? null : criteriaBuilder.equal(root.get("parent"), parent);
    }

    public static Specification<OrgUnit> hasUserWithUsername(String login) {
        return (root, query, criteriaBuilder) -> {
            Join<OrgUnit, Assignment> assignmentsJoin = root.join("assignments", JoinType.INNER);
            Join<Assignment, Team> teamJoin = assignmentsJoin.join("team", JoinType.INNER);
            Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);

            return criteriaBuilder.equal(userJoin.get("login"), login);
        };
    }
}
