package org.nmcpye.datarun.useraccess.accessfilter;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central Access Filter Registry
 *
 * @author Hamza Assada, 21/03/2025
 */
@Component
public class AccessFilterRegistry {

    private final List<AccessFilter<? extends AuditableObject<?>>> filterBeans;
    private final Map<Class<? extends AuditableObject<?>>, AccessFilter<? extends AuditableObject<?>>> filters = new ConcurrentHashMap<>();

    public AccessFilterRegistry(List<AccessFilter<? extends AuditableObject<?>>> filterBeans) {
        this.filterBeans = filterBeans;
    }

    public void registerFilters() {
        if (!filters.isEmpty()) {
            return;
        }
        filterBeans.forEach(filter ->
            filters.put(filter.getKlass(), filter)
        );
    }

    private AccessFilter<?> getFilter(Class<?> klass) {
        registerFilters();
        return filters.get(klass);
    }

    @SuppressWarnings("unchecked")
    public <T extends AuditableObject<?>> Specification<T> getSpecification(
        Class<T> entityClass, CurrentUserDetails user, QueryRequest queryRequest) {
        AccessFilter<T> filter = (AccessFilter<T>) getFilter(entityClass);
        final var querySpecification = (Specification<T>) AccessFilter.buildQuerySpecification(queryRequest);
        return querySpecification.and(filter != null ?
            filter.getAccessSpecification(user, queryRequest) :
            AccessFilter.createDefaultSpecification(user));
    }
}
