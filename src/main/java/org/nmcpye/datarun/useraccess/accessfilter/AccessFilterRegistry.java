package org.nmcpye.datarun.useraccess.accessfilter;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central Access Filter Registry
 *
 * @author Hamza, 21/03/2025
 */
@Component
public class AccessFilterRegistry {

    private final List<AccessFilter<?>> filterBeans;
    private final Map<Class<?>, AccessFilter<?>> filters = new ConcurrentHashMap<>();

    public AccessFilterRegistry(List<AccessFilter<?>> filterBeans) {
        this.filterBeans = filterBeans;
    }

    public void registerFilters() {
        if (!filters.isEmpty()) {
            return;
        }
        filterBeans.forEach(filter ->
            filters.put(filter.getClazz(), filter)
        );
    }

    private AccessFilter<?> getFilter(Class<?> klass) {
        registerFilters();
        return filters.get(klass);
    }

    @SuppressWarnings("unchecked")
    public <T extends AuditableObject<?>> Specification<T> getSpecification(Class<T> entityClass, CurrentUserDetails user, boolean includeDisabled) {
        AccessFilter<T> filter = (AccessFilter<T>) getFilter(entityClass);
        return filter != null ?
            filter.createSpecification(user, includeDisabled) :
            AccessFilter.createDefaultSpecification(user);
    }
}
