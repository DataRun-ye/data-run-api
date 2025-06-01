package org.nmcpye.datarun.common.jpa;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.impl.SoftDeleteService;
import org.nmcpye.datarun.common.jpa.repository.JpaAuditableRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.useraccess.UserAccessService;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author Hamza Assada, 20/03/2025
 */
@Slf4j
public abstract class DefaultJpaSoftDeleteService
    <T extends JpaSoftDeleteObject>
    extends DefaultJpaAuditableService<T>
    implements SoftDeleteService<T, Long> {

    protected final UserAccessService userAccessService;
    protected final JpaAuditableRepository<T> jpaAuditableObjectRepository;

    public DefaultJpaSoftDeleteService(JpaAuditableRepository<T> jpaAuditableObjectRepository, CacheManager cacheManager, UserAccessService userAccessService, UserAccessService userAccessService1, JpaAuditableRepository<T> jpaAuditableObjectRepository1) {
        super(jpaAuditableObjectRepository, cacheManager, userAccessService);
        this.userAccessService = userAccessService1;
        this.jpaAuditableObjectRepository = jpaAuditableObjectRepository1;
    }

    @Override
    public void deleteByUid(String uid) {
        log.debug("Request soft delete service to soft delete {}:`{}`", getClazz().getSimpleName(), uid);
        final var toMarkAsDeleted = findByUid(uid).orElseThrow(() ->
            new IllegalQueryException(new ErrorMessage(ErrorCode.E1106, "Id", uid)));
        toMarkAsDeleted.setDeleted(Boolean.TRUE);
        save(toMarkAsDeleted);
    }

    @Override
    public void delete(T object) {
        log.debug("Request soft delete service to soft delete {}:`{}`", getClazz().getSimpleName(), object.getUid());
        final var toMarkAsDeleted = findByUid(object.getUid()).orElseThrow(() ->
            new IllegalQueryException(new ErrorMessage(ErrorCode.E1109,
                object.getClass().getSimpleName(), object.getUid())));
        toMarkAsDeleted.setDeleted(Boolean.TRUE);
        save(toMarkAsDeleted);
    }

    protected Specification<T> baseAccessSpecification(CurrentUserDetails user, QueryRequest queryRequest) {
        Specification<T> accessSpec = userAccessService.readSpec(getClazz(), user, queryRequest);
        if (queryRequest == null || !queryRequest.isIncludeDeleted()) {
            return accessSpec.and(includeDeletedCheckSpecification());
        }
        return accessSpec;
    }

    private Specification<T> includeDeletedCheckSpecification() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.isFalse(root.get("deleted"));
    }
}
