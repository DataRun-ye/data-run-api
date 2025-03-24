package org.nmcpye.datarun.common.jpa;

import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Hamza, 20/03/2025
 */
public interface JpaAuditableObjectService<T extends JpaAuditableObject>
    extends AuditableObjectService<T, Long> {
    @Deprecated
    Page<T> findAllByUser(Specification<T> spec, Pageable pageable, QueryRequest queryRequest);
}
