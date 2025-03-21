package org.nmcpye.datarun.useraccess.accessfilter;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * @author Hamza, 21/03/2025
 */
@Component
public class DefaultJpaFilter<T extends AuditableObject<?>>
    implements AccessFilter<T> {
    private final Class<T> clazz;

    public DefaultJpaFilter(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<T> getClazz() {
        return clazz;
    }

    @Override
    public Specification<T> createSpecification(CurrentUserDetails user, boolean includeDisabled) {
        return AccessFilter.createDefaultSpecification(user);
    }
}
