package org.nmcpye.datarun.security.useraccess.accessfilter;

import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada, 21/03/2025
 */
@Component
public class ProjectFilter extends DefaultJpaFilter<Project> {
    @Override
    public Specification<Project> getAccessSpecification(CurrentUserDetails user,
                                                         QueryRequest queryRequest) {
        return (root, query, cb) -> cb.isFalse(root.get("disabled"));
    }
}
