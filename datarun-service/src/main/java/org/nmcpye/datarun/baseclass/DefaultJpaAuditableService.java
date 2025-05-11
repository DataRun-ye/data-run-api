package org.nmcpye.datarun.baseclass;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.impl.DefaultAuditableObjectService;
import org.nmcpye.datarun.common.jpa.JpaAuditableObject;
import org.nmcpye.datarun.common.jpa.JpaAuditableObjectService;
import org.nmcpye.datarun.common.jpa.repository.JpaAuditableRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.security.useraccess.UserAccessService;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hamza Assada, 20/03/2025
 */
public abstract class DefaultJpaAuditableService
    <T extends JpaAuditableObject>
    extends DefaultAuditableObjectService<T, Long>
    implements JpaAuditableObjectService<T> {
    private static final Logger log = LoggerFactory.getLogger(DefaultJpaAuditableService.class);

    protected final UserAccessService userAccessService;
    protected final JpaAuditableRepository<T> jpaAuditableObjectRepository;

    public DefaultJpaAuditableService(JpaAuditableRepository<T> jpaAuditableObjectRepository,
                                      CacheManager cacheManager, UserAccessService userAccessService) {
        super(jpaAuditableObjectRepository, cacheManager);
        this.userAccessService = userAccessService;
        this.jpaAuditableObjectRepository = jpaAuditableObjectRepository;
    }

    protected Specification<T> baseSpecification(CurrentUserDetails user, QueryRequest queryRequest) {
        return userAccessService.readSpec(getClazz(), user, queryRequest);
    }

    @Override
    public Page<T> findAllByUser(QueryRequest queryRequest) {
        final var user = SecurityUtils.getCurrentUserDetails();
        final var spec = baseSpecification(user
            .orElseThrow(() -> new IllegalQueryException(new ErrorMessage(ErrorCode.E6201))), queryRequest);
        return jpaAuditableObjectRepository.findAll(spec, queryRequest.getPageable());
    }

//    @Override
//    public List<T> findAllByUser(QueryRequest queryRequest) {
//        final var readSpec = baseSpecification(SecurityUtils.getCurrentUserDetails()
//                .orElseThrow(() -> new IllegalQueryException(new ErrorMessage(ErrorCode.E6201))),
//            queryRequest);
//        return jpaAuditableObjectRepository.findAll(readSpec);
//    }

    @Override
    public Page<T> findAllByUser(Specification<T> spec, QueryRequest queryRequest) {
        Pageable pageable = queryRequest.getPageable();
        final var accessSpec = baseSpecification(
            SecurityUtils.getCurrentUserDetails()
                .orElseThrow(() ->
                    new IllegalQueryException(new ErrorMessage(ErrorCode.E6201))),
            queryRequest);
        return jpaAuditableObjectRepository.findAll(spec.and(accessSpec), pageable);
    }

    @Override
    @Transactional
    public T update(T object) {
        log.debug("Request service to update {}:`{}`", getClazz().getSimpleName(), object.getUid());
        T existingEntity = findByIdentifyingProperties(object)
            .orElseThrow(() ->
                new IllegalQueryException(
                    new ErrorMessage(ErrorCode.E1004,
                        getClazz().getSimpleName(), object.getUid())));

        object.setId(existingEntity.getId());
        object.setCreatedBy(existingEntity.getCreatedBy());

        object.setIsPersisted();
        /// update object, overwrite with updates
        return saveWithRelations(object);
    }
}
