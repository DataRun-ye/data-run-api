package org.nmcpye.datarun.useraccess.accessfilter;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic Access Filter Interface, initial temporary
 * step before transitioning to ABAC
 *
 * @author Hamza, 21/03/2025
 */
public interface AccessFilter<T extends AuditableObject<?>> {
    Class<T> getKlass();

    Specification<T> getAccessSpecification(CurrentUserDetails user, QueryRequest queryRequest);

    static <E extends AuditableObject<?>> Specification<E> buildQuerySpecification(QueryRequest queryRequest) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            queryRequest.getFilters().forEach((key, value) -> {
                if (key.contains(".")) {
                    // Handle nested properties, for example: parent.uid
                    String[] parts = key.split("\\.");
                    Path<Object> path = root.get(parts[0]);
                    for (int i = 1; i < parts.length; i++) {
                        path = path.get(parts[i]);
                    }
                    predicates.add(cb.equal(path, value));
                } else {
                    // Handle simple properties
                    predicates.add(cb.equal(root.get(key), value));
                }
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

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
