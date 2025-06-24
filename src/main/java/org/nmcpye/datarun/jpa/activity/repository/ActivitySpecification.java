package org.nmcpye.datarun.jpa.activity.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Hamza Assada 11/02/2022
 */
public abstract class ActivitySpecification {

    public static Specification<Activity> canRead() {
        return (root, query, criteriaBuilder) -> {
            if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
                return criteriaBuilder.conjunction();
            } else if (!SecurityUtils.isAuthenticated()) {
                return criteriaBuilder.disjunction();
            } else {
                String currentUserLogin = SecurityUtils.getCurrentUserLoginOrThrow(
                    new ErrorMessage(ErrorCode.E3004, Activity.class.getName()));

                Join<Activity, Assignment> assignmentJoin = root.join("assignments", JoinType.INNER);
                Join<Assignment, Team> teamJoin = assignmentJoin.join("team", JoinType.INNER);
                Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);

                return criteriaBuilder.equal(userJoin.get("login"), currentUserLogin);
            }
        };
    }

    public static Specification<Activity> isEnabled() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("disabled"));
    }

    public static Specification<Activity> canReadAndIsEnabled() {
        return canRead().and(isEnabled());
    }
}
