package org.nmcpye.datarun.drun.postgres.common;

import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

public interface IdentifiableAccessSpecification<T extends Identifiable<Long>> {
    default Specification<T> canRead() {
        return (root, query, criteriaBuilder) -> {
            if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
                return criteriaBuilder.conjunction();
            } else if (!SecurityUtils.isAuthenticated()) {
                return null;
            } else {
                String currentUserLogin = SecurityUtils.getCurrentUserLogin()
                    .orElseThrow(() -> new IllegalStateException("Current user login not found"));
                return currentUserLogin == null ? null : criteriaBuilder.equal(root.get("createdBy"), currentUserLogin);
            }
        };
    }
}
