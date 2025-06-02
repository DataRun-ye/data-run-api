package org.nmcpye.datarun.jpa.accessfilter;

import org.nmcpye.datarun.jpa.oulevel.OuLevel;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * @author Hamza Assada, 21/03/2025
 */
@Component
public class OuLevelFilter extends DefaultJpaFilter<OuLevel> {

    @Override
    public Specification<OuLevel> getAccessSpecification(CurrentUserDetails user,
                                                         QueryRequest queryRequest) {
        return (root, query, cb) -> cb.conjunction();
    }
}
