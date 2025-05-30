package org.nmcpye.datarun.drun.postgres.common;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.activity.Activity;
import org.nmcpye.datarun.user.User;
import org.nmcpye.datarun.team.Team;
import org.nmcpye.datarun.assignment.Assignment;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

public abstract class ActivitySpecifications {

    public static Specification<Activity> canRead() {
        return (root, query, criteriaBuilder) -> {
            Join<Activity, Assignment> assignmentJoin = root.join("assignments", JoinType.INNER);
            Join<Assignment, Team> teamJoin = assignmentJoin.join("team", JoinType.INNER);
            Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);

            String currentUserLogin = SecurityUtils.getCurrentUserLoginOrThrow(
                new ErrorMessage(ErrorCode.E3004, Activity.class.getName()));

            return criteriaBuilder.equal(userJoin.get("login"), currentUserLogin);
        };
    }

    public static Specification<Activity> isEnabled() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("disabled"));
    }
}

