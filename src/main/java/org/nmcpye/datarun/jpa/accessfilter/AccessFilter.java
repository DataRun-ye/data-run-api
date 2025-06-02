package org.nmcpye.datarun.jpa.accessfilter;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;

/**
 * Generic Access Filter Interface, initial temporary
 * step before transitioning to ABAC
 *
 * @author Hamza Assada, 21/03/2025
 */
public interface AccessFilter<T extends AuditableObject<?>> {
    Class<T> getKlass();

    Specification<T> getAccessSpecification(CurrentUserDetails user, QueryRequest queryRequest);

    static <E extends AuditableObject<?>> Specification<E> createDefaultSpecification(CurrentUserDetails user) {
        return (root, query, criteriaBuilder) -> {
            if (user.isSuper()) {
                return criteriaBuilder.conjunction();
            } else {
                return criteriaBuilder.equal(root.get("createdBy"), user.getUsername());
            }
        };
    }
}
