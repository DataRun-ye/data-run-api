package org.nmcpye.datarun.jpa.common;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.common.IdentifiableObjectService;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada
 * @since 20/03/2025
 */
public interface JpaIdentifiableObjectService<T extends JpaIdentifiableObject>
    extends IdentifiableObjectService<T, String> {

//    static <T extends JpaIdentifiableObject> Specification<T> hasUid(String uid) {
//        return (root, query, criteriaBuilder) -> uid == null ?
//            criteriaBuilder.disjunction()
//            : criteriaBuilder.equal(root.get("uid"), uid);
//    }

    static <T extends JpaIdentifiableObject> Specification<T> hasId(String id) {
        return (root, query, criteriaBuilder) -> id == null ?
            criteriaBuilder.disjunction()
            : criteriaBuilder.equal(root.get("id"), id);
    }

    @Deprecated(since = "v 6 use mongo like json query")
    static <E extends JpaIdentifiableObject> Specification<E> buildQuerySpecification(QueryRequest queryRequest) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            queryRequest.getFilters().forEach((key, value) -> {
                if (key.contains(".")) {
                    // Handle nested properties, for example: parent.id
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
