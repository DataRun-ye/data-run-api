package org.nmcpye.datarun.jpa.accessfilter;

import org.nmcpye.datarun.common.IdentifiableObject;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.query.QueryRequest;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Hamza Assada
 * @since 20/03/2025
 */
public interface UserAccessService {
    <T extends IdentifiableObject<?>> Specification<T> readSpec(Class<T> klass, CurrentUserDetails user, QueryRequest queryRequest);
}
