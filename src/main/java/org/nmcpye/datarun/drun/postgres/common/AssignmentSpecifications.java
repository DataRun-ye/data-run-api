package org.nmcpye.datarun.drun.postgres.common;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

public abstract class AssignmentSpecifications {


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
