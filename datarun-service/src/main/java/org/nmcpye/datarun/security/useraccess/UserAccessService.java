package org.nmcpye.datarun.security.useraccess;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Hamza Assada, 20/03/2025
 */
public interface UserAccessService {
    <T extends AuditableObject<?>> Specification<T> readSpec(Class<T> klass, CurrentUserDetails user, QueryRequest queryRequest);
}
