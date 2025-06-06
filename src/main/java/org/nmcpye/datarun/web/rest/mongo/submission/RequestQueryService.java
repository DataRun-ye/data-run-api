package org.nmcpye.datarun.web.rest.mongo.submission;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.common.AuditableObject;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada 22/03/2025 <7amza.it@gmail.com>
 */
@SuppressWarnings("unchecked")
public class RequestQueryService<T extends AuditableObject<?>> {
    private final Class<T> klass;

    public RequestQueryService(Class<T> klass) {
        this.klass = (Class<T>) ((ParameterizedType) getClass()
            .getGenericSuperclass())
            .getActualTypeArguments()[0];
    }

    public Class<T> getKlass() {
        return klass;
    }

    public Specification<T> buildForJpa(QueryRequest queryRequest) {
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
