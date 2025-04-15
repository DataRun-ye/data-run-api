package org.nmcpye.datarun.useraccess;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.useraccess.accessfilter.AccessFilterRegistry;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * @author Hamza Assada, 20/03/2025
 */
@Service
public class UserAccessServiceImpl
    implements UserAccessService {
    private final AccessFilterRegistry accessFilterRegistry;

    public UserAccessServiceImpl(AccessFilterRegistry accessFilterRegistry) {
        this.accessFilterRegistry = accessFilterRegistry;
    }

    @Override
    public <T extends AuditableObject<?>> Specification<T> readSpec(Class<T> klass, CurrentUserDetails user,  QueryRequest queryRequest) {
        return accessFilterRegistry.getSpecification(klass, user, queryRequest);
    }
}
