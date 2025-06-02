package org.nmcpye.datarun.jpa.usegroup.repository;

import jakarta.persistence.criteria.*;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.usegroup.UserGroup;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

public abstract class UserGroupSpecifications {

    public static Specification<UserGroup> canRead() {
        return (root, query, criteriaBuilder) -> {
            if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
                return criteriaBuilder.conjunction();
            } else if (!SecurityUtils.isAuthenticated()) {
                return criteriaBuilder.disjunction();
            } else {
                String currentUserLogin = SecurityUtils.getCurrentUserLoginOrThrow(
                    new ErrorMessage(ErrorCode.E3004, UserGroup.class.getName()));
                Join<UserGroup, User> userJoin = root.join("users", JoinType.INNER);
                return criteriaBuilder.equal(userJoin.get("login"), currentUserLogin);
            }
        };
    }

    public static Specification<UserGroup> isEnabled() {
        return (root, query, criteriaBuilder) -> {

            Predicate userGroupNotDisabled = criteriaBuilder.isFalse(root.get("disabled"));

            return criteriaBuilder.and(userGroupNotDisabled);
        };
    }

    // Specification to get groups managed by groups that a user is part of
    public static Specification<UserGroup> getManagedGroupsByUserGroups(String userLogin) {
        return (root, query, cb) -> {
            Subquery<UserGroup> userGroupSubquery = query.subquery(UserGroup.class);
            Root<UserGroup> userGroupRoot = userGroupSubquery.from(UserGroup.class);
            Join<UserGroup, User> userJoin = userGroupRoot.join("users", JoinType.INNER);

            userGroupSubquery.select(userGroupRoot)
                .where(cb.equal(userJoin.get("login"), userLogin));

            Join<UserGroup, UserGroup> managedByJoin = root.join("managedByGroups", JoinType.INNER);
            return managedByJoin.in(userGroupSubquery);
        };
    }
}

