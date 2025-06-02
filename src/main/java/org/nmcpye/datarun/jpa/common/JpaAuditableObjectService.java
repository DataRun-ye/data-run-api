package org.nmcpye.datarun.jpa.common;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada, 20/03/2025
 */
public interface JpaAuditableObjectService<T extends AuditableObject<Long>>
    extends AuditableObjectService<T, Long> {

//    boolean canWrite(Specification<T> spec, QueryRequest queryRequest);

    static <T extends AuditableObject<Long>> Specification<T> hasUid(String uid) {
        return (root, query, criteriaBuilder) -> uid == null ? criteriaBuilder.disjunction()
            : criteriaBuilder.equal(root.get("uid"), uid);
    }

    @Deprecated(since = "v 6 use mongo like json query")
    static <E extends AuditableObject<Long>> Specification<E> buildQuerySpecification(QueryRequest queryRequest) {
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
}
