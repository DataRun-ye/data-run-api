package org.nmcpye.datarun.jpa.accessfilter;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.springframework.data.jpa.domain.Specification;

/**
 * Entity-read visibility filter used by generic read services.
 *
 * @author Hamza Assada
 * @since 21/03/2025
 */
public interface AccessFilter<T extends AuditableObject<?>> {
    Class<T> getKlass();

    Specification<T> getAccessSpecification(CurrentUserDetails user, QueryRequest queryRequest);

    /**
     * Compatibility policy for generic resources that do not yet have an
     * explicit domain visibility filter. Retire it by classifying each such
     * route, then registering a domain filter or removing the route.
     */
    static <E extends AuditableObject<?>> Specification<E> createCompatibilitySpecification(CurrentUserDetails user) {
        return (root, query, criteriaBuilder) -> {
            if (user.isSuper()) {
                return criteriaBuilder.conjunction();
            } else {
                return criteriaBuilder.equal(root.get("createdBy"), user.getUsername());
            }
        };
    }
}
