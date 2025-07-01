package org.nmcpye.datarun.jpa.accessfilter;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.ParameterizedType;

/**
 * @author Hamza Assada 21/03/2025 (7amza.it@gmail.com)
 */
@SuppressWarnings("unchecked")
public class DefaultJpaFilter<T extends AuditableObject<?>>
    implements AccessFilter<T> {
    private final Class<T> klass;

    public DefaultJpaFilter() {
        this.klass = (Class<T>) ((ParameterizedType) getClass()
            .getGenericSuperclass())
            .getActualTypeArguments()[0];
    }

    @Override
    public Class<T> getKlass() {
        return klass;
    }

    @Override
    public Specification<T> getAccessSpecification(CurrentUserDetails user,
                                                   QueryRequest queryRequest) {
        return AccessFilter.createDefaultSpecification(user);
    }
}
