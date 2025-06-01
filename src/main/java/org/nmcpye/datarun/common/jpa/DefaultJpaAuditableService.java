package org.nmcpye.datarun.common.jpa;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.impl.DefaultAuditableObjectService;
import org.nmcpye.datarun.common.jpa.repository.JpaAuditableRepository;
import org.nmcpye.datarun.query.JpaQueryBuilder;
import org.nmcpye.datarun.query.UnifiedQueryParser;
import org.nmcpye.datarun.query.filter.FilterExpression;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.security.useraccess.UserAccessService;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.nmcpye.datarun.common.jpa.JpaAuditableObjectService.buildQuerySpecification;

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

    @Autowired
    protected JpaQueryBuilder<T> jpaQueryBuilder;

    public DefaultJpaAuditableService(JpaAuditableRepository<T> jpaAuditableObjectRepository,
                                      CacheManager cacheManager, UserAccessService userAccessService) {
        super(jpaAuditableObjectRepository, cacheManager);
        this.userAccessService = userAccessService;
        this.jpaAuditableObjectRepository = jpaAuditableObjectRepository;
    }

    protected Specification<T> baseAccessSpecification(CurrentUserDetails user, QueryRequest queryRequest) {
        return userAccessService.readSpec(getClazz(), user, queryRequest);
    }

    @Override
    public Optional<T> findByUid(String uid) {
        final var user = SecurityUtils.getCurrentUserDetails();
        final var spec = baseAccessSpecification(user
            .orElseThrow(() -> new IllegalQueryException(new ErrorMessage(ErrorCode.E6201))), null)
            .and(JpaAuditableObjectService.hasUid(uid));
        return jpaAuditableObjectRepository.findOne(spec);
    }

    @Override
    public boolean existsByUid(String uid) {
        final var user = SecurityUtils.getCurrentUserDetails();

        final var spec = baseAccessSpecification(user
            .orElseThrow(() -> new IllegalQueryException(new ErrorMessage(ErrorCode.E6201))), null)
            .and(JpaAuditableObjectService.hasUid(uid));
        return jpaAuditableObjectRepository.exists(spec);
    }

    @Override
    public Page<T> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        final var user = SecurityUtils.getCurrentUserDetails();
        final var spec = baseAccessSpecification(user
            .orElseThrow(() -> new IllegalQueryException(new ErrorMessage(ErrorCode.E6201))), queryRequest)
            .and(buildQuerySpecification(queryRequest))
            .and(buildJsonQuerySpecification(jsonQueryBody));
        return jpaAuditableObjectRepository.findAll(spec, queryRequest.getPageable());
    }

    protected Specification<T> buildJsonQuerySpecification(String jsonQueryBody) {
        Specification<T> spec = Specification.where(null);
        if (jsonQueryBody != null && !jsonQueryBody.isEmpty()) {
            try {
                FilterExpression filterExpression = UnifiedQueryParser.parse(jsonQueryBody);
                spec = spec.and(jpaQueryBuilder.buildQuery(List.of(filterExpression)));
            } catch (Exception e) {
                throw new IllegalQueryException(ErrorCode.E2050, jsonQueryBody);
            }
        }
        return spec;
    }
    
    @Override
    @Transactional
    public T update(T object) {
        log.debug("Request service to update {}:`{}`", getClazz().getSimpleName(), object.getUid());
        final var user = SecurityUtils.getCurrentUserDetails();
        final var spec = baseAccessSpecification(user
            .orElseThrow(() -> new IllegalQueryException(new ErrorMessage(ErrorCode.E6201))), null)
            .and(JpaAuditableObjectService.hasUid(object.getUid()));

        T existingEntity = jpaAuditableObjectRepository.findOne(spec)
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
