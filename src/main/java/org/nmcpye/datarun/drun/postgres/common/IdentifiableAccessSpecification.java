package org.nmcpye.datarun.drun.postgres.common;


import org.nmcpye.datarun.common.IdentifiableObject;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

public class IdentifiableAccessSpecification {
    public static <T extends IdentifiableObject<Long>> Specification<T> canRead() {
        return (root, query, criteriaBuilder) -> {
            if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
                return criteriaBuilder.conjunction();
            } else if (!SecurityUtils.isAuthenticated()) {
                return criteriaBuilder.disjunction();
            } else {
                String currentUserLogin = SecurityUtils.getCurrentUserLoginOrThrow(
                    new ErrorMessage(ErrorCode.E3005, IdentifiableObject.class.getName()));
                return currentUserLogin == null ? criteriaBuilder.disjunction()
                    : criteriaBuilder.equal(root.get("createdBy"), currentUserLogin);
            }
        };
    }

    public static <T extends IdentifiableObject<Long>> Specification<T> hasUid(String uid) {
        return (root, query, criteriaBuilder) -> uid == null ? criteriaBuilder.disjunction()
            : criteriaBuilder.equal(root.get("uid"), uid);
    }

    public static <T extends IdentifiableObject<Long>> Specification<T> hasCode(String code) {
        return (root, query, criteriaBuilder) -> code == null ? criteriaBuilder.disjunction()
            : criteriaBuilder.equal(root.get("code"), code);
    }
}
