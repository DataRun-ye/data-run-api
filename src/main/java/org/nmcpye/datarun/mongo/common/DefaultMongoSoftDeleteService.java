package org.nmcpye.datarun.mongo.common;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.AuditableObjectRepository;
import org.nmcpye.datarun.common.SoftDeleteService;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.query.UnifiedQueryParser;
import org.nmcpye.datarun.query.filter.*;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada, 20/03/2025
 */
@Slf4j
public abstract class DefaultMongoSoftDeleteService<T extends MongoSoftDeleteObject>
    extends DefaultMongoAuditableObjectService<T>
    implements SoftDeleteService<T, String> {


    public DefaultMongoSoftDeleteService(AuditableObjectRepository<T, String> repository, CacheManager cacheManager) {
        super(repository, cacheManager);
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

    @Override
    protected FilterExpression buildCombinedFilter(QueryRequest queryRequest, String jsonQueryBody) {
        List<FilterExpression> allFilters = new ArrayList<>();

        // Implicit soft-delete filter
        if (queryRequest == null || !queryRequest.isIncludeDeleted()) {
            allFilters.add(new SimpleFilter("deleted", FilterOperator.EQ, false));
        }

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
        if (queryRequest != null && queryRequest.getParsedFilter() != null) {
            allFilters.add(legacyQueryConverter.convert(queryRequest));
        }

        if (allFilters.isEmpty()) return null;
        if (allFilters.size() == 1) return allFilters.get(0);

        return new CompoundFilter(LogicalOperator.AND, allFilters);
    }
}
