package org.nmcpye.datarun.jpa.common;

import org.nmcpye.datarun.common.DefaultIdentifiableObjectService;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.query.JpaQueryBuilder;
import org.nmcpye.datarun.query.LegacyQueryConverter;
import org.nmcpye.datarun.query.UnifiedQueryParser;
import org.nmcpye.datarun.query.filter.*;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Hamza Assada 20/03/2025 (7amza.it@gmail.com)
 */
public abstract class DefaultJpaIdentifiableService
    <T extends JpaIdentifiableObject>
    extends DefaultIdentifiableObjectService<T, String>
    implements JpaIdentifiableObjectService<T> {
    private static final Logger log = LoggerFactory.getLogger(DefaultJpaIdentifiableService.class);

    protected final UserAccessService userAccessService;
    protected final JpaIdentifiableRepository<T> identifiableRepository;

    @Autowired
    protected JpaQueryBuilder<T> jpaQueryBuilder;

    @Autowired
    protected LegacyQueryConverter legacyQueryConverter;

    public DefaultJpaIdentifiableService(JpaIdentifiableRepository<T> jpaIdentifiableRepository,
                                         CacheManager cacheManager, UserAccessService userAccessService) {
        super(jpaIdentifiableRepository, cacheManager);
        this.userAccessService = userAccessService;
        this.identifiableRepository = jpaIdentifiableRepository;
    }

    protected Specification<T> baseAccessSpecification(CurrentUserDetails user, QueryRequest queryRequest,
                                                       String jsonQueryBody) {
        var accessSpec = userAccessService.readSpec(getClazz(), user, queryRequest);
        FilterExpression combinedFilter = buildCombinedFilter(queryRequest, jsonQueryBody);
        // add the 'since' filter
        // only if it's not the epoch sentinel
        if (queryRequest.getSince() != null && !queryRequest.getSince().equals(Instant.EPOCH)) {
            // assuming your field is called "lastModifiedDate"
            SimpleFilter sinceFilter = new SimpleFilter(
                "lastModifiedDate", FilterOperator.GT, queryRequest.getSince());
            combinedFilter = (combinedFilter == null)
                ? sinceFilter
                : new CompoundFilter(LogicalOperator.AND,
                List.of(combinedFilter, sinceFilter));
        }

        if (combinedFilter != null) {
            accessSpec = accessSpec.and(jpaQueryBuilder.buildQuery(List.of(combinedFilter)));
        }
        return accessSpec;
    }

    protected FilterExpression buildCombinedFilter(QueryRequest queryRequest, String jsonQueryBody) {
        List<FilterExpression> allFilters = new ArrayList<>();

        // v2 JSON expression support
        if (jsonQueryBody != null && !jsonQueryBody.isEmpty()) {
            try {
                FilterExpression parsed = UnifiedQueryParser.parse(jsonQueryBody);
                allFilters.add(parsed);
            } catch (Exception e) {
                throw new IllegalQueryException(ErrorCode.E2050, jsonQueryBody);
            }
        }

        // legacy v1 QueryRequest support
        if (queryRequest != null && queryRequest.getParsedFilter() != null &&
            !queryRequest.getParsedFilter().isEmpty()) {
            allFilters.add(legacyQueryConverter.convert(queryRequest));
        }

        if (allFilters.isEmpty()) return null;
        if (allFilters.size() == 1) return allFilters.get(0);

        return new CompoundFilter(LogicalOperator.AND, allFilters);
    }

    @Override
    public Page<T> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        final Specification<T> query = baseAccessSpecification(SecurityUtils.getCurrentUserDetailsOrThrow(),
            queryRequest, jsonQueryBody);
        final var list = identifiableRepository.findAll(query, queryRequest.getPageable());
        return list;
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
        log.debug("Request service to update {}:`{}`", getClazz().getSimpleName(), object.getId());
        T existingEntity = findByIdOrUid(object)
            .orElseThrow(() ->
                new IllegalQueryException(
                    new ErrorMessage(ErrorCode.E1004,
                        getClazz().getSimpleName(), Optional
                        .ofNullable(object.getId()).orElse(object.getUid()))));

        object.setId(existingEntity.getId());

        object.setIsPersisted();
        /// update object, overwrite with updates
        return saveWithRelations(object);
    }

    @Override
    public Optional<T> findByIdOrUid(T entity) {
        return Optional.ofNullable(entity.getId())
            .flatMap(repository::findById)
            .or(() -> Optional.ofNullable(entity.getUid())
                .flatMap(repository::findByUid));
    }

    @Override
    public Optional<T> findByIdOrUid(String entity) {
        return Optional.ofNullable(entity)
            .flatMap(repository::findById)
            .or(() -> Optional.ofNullable(entity)
                .flatMap(repository::findByUid));
    }
}

