package org.nmcpye.datarun.web.rest.common;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public interface QuerySpecification<T> {
    default Specification<T> buildQuerySpecification(QueryRequest queryRequest) {
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
