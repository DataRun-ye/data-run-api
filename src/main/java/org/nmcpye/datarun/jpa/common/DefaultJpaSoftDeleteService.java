package org.nmcpye.datarun.jpa.common;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.SoftDeleteService;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.query.filter.*;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada
 * @since 20/03/2025
 */
@Slf4j
public abstract class DefaultJpaSoftDeleteService
    <T extends JpaSoftDeleteObject>
    extends DefaultJpaIdentifiableService<T>
    implements SoftDeleteService<T, String> {

    protected final UserAccessService userAccessService;
    protected final JpaIdentifiableRepository<T> jpaAuditableObjectRepository;

    public DefaultJpaSoftDeleteService(JpaIdentifiableRepository<T> jpaAuditableObjectRepository,
                                       CacheManager cacheManager, UserAccessService userAccessService) {
        super(jpaAuditableObjectRepository, cacheManager, userAccessService);
        this.userAccessService = userAccessService;
        this.jpaAuditableObjectRepository = jpaAuditableObjectRepository;
    }

    @Override
    public T save(T object) {
        if (!object.getDeleted()) {
            object.setDeletedAt(null);
        }

        return super.save(object);
    }

    @Transactional
    @Override
    public void deleteByUid(String uid) {
        log.debug("Request soft delete service to soft delete {}:`{}`", getClazz().getSimpleName(), uid);
        findByIdOrUid(uid).ifPresent(this::softDelete);
    }


    @Transactional
    @Override
    public void delete(T object) {
        log.debug("Request soft delete service to soft delete {}:`{}`", getClazz().getSimpleName(), object.getUid());
        findByIdOrUid(object).ifPresent(this::softDelete);
    }

    @Override
    public void softDelete(T object) {
        object.setDeleted(Boolean.TRUE);
        object.setDeletedAt(Instant.now());
        save(object);
    }

    protected FilterExpression buildCombinedFilter(QueryRequest queryRequest, String jsonQueryBody) {
        List<FilterExpression> allFilters = new ArrayList<>();
        final FilterExpression baseFilter = super.buildCombinedFilter(queryRequest, jsonQueryBody);
        if (baseFilter != null) {
            allFilters.add(baseFilter);
        }

        /// ///
        // Implicit soft-delete filter
        if (queryRequest == null || !queryRequest.isIncludeDeleted()) {
            allFilters.add(new SimpleFilter("deleted", FilterOperator.EQ, false));
        }
        //

        if (allFilters.isEmpty()) return null;
        if (allFilters.size() == 1) return allFilters.get(0);

        return new CompoundFilter(LogicalOperator.AND, allFilters);
    }
}
