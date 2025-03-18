package org.nmcpye.datarun.drun.postgres.common;

import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

public interface IdentifiableAccessSpecification<T extends IdentifiableEntity<Long>> {
    default Specification<T> canRead() {
        return (root, query, criteriaBuilder) -> {
            if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
                return criteriaBuilder.conjunction();
            } else if (!SecurityUtils.isAuthenticated()) {
                return null;
            } else {
                String currentUserLogin = SecurityUtils.getCurrentUserLoginOrThrow(
                    new ErrorMessage(ErrorCode.E3004, Identifiable.class.getName()));
                return currentUserLogin == null ? null : criteriaBuilder.equal(root.get("createdBy"), currentUserLogin);
            }
        };
    }
}
