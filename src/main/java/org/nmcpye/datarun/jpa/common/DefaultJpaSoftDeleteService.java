package org.nmcpye.datarun.jpa.common;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.SoftDeleteService;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.repository.JpaAuditableRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

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

    public DefaultJpaSoftDeleteService(JpaAuditableRepository<T> jpaAuditableObjectRepository,
                                       CacheManager cacheManager, UserAccessService userAccessService) {
        super(jpaAuditableObjectRepository, cacheManager, userAccessService);
        this.userAccessService = userAccessService;
        this.jpaAuditableObjectRepository = jpaAuditableObjectRepository;
    }

    @Override
    public T save(T object) {
        log.debug("Request service to save {}:`{}`", getClazz().getSimpleName(), object.getUid());
        if (!object.getDeleted() && object.getDeletedAt() != null) {
            object.setDeletedAt(null);
        }

        return repository.save(object);
    }

    @Override
    public void deleteByUid(String uid) {
        log.debug("Request soft delete service to soft delete {}:`{}`", getClazz().getSimpleName(), uid);
        final var toMarkAsDeleted = findByUid(uid).orElseThrow(() ->
            new IllegalQueryException(new ErrorMessage(ErrorCode.E1106, "Id", uid)));
        toMarkAsDeleted.setDeleted(Boolean.TRUE);
        toMarkAsDeleted.setDeletedAt(Instant.now());
        save(toMarkAsDeleted);
    }

    @Override
    public void delete(T object) {
        log.debug("Request soft delete service to soft delete {}:`{}`", getClazz().getSimpleName(), object.getUid());
        final var toMarkAsDeleted = findByUid(object.getUid()).orElseThrow(() ->
            new IllegalQueryException(new ErrorMessage(ErrorCode.E1109,
                object.getClass().getSimpleName(), object.getUid())));
        toMarkAsDeleted.setDeleted(Boolean.TRUE);
        toMarkAsDeleted.setDeletedAt(Instant.now());
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
