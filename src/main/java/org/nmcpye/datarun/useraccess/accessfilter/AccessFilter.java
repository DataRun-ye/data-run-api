package org.nmcpye.datarun.useraccess.accessfilter;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.data.jpa.domain.Specification;

/**
 * Generic Access Filter Interface, initial temporary
 * step before transitioning to ABAC
 *
 * @author Hamza, 21/03/2025
 */
public interface AccessFilter<T extends AuditableObject<?>> {
    static <E extends AuditableObject<?>> Specification<E> createDefaultSpecification(CurrentUserDetails user) {
        return (root, query, criteriaBuilder) -> {
            if (user.isSuper()) {
                return criteriaBuilder.conjunction();
            } else {
                return criteriaBuilder.equal(root.get("createdBy"), user.getUsername());
            }
        };
    }

    Class<T> getClazz();

    Specification<T> createSpecification(CurrentUserDetails user, boolean includeDisabled);
}
