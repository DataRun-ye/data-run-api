package org.nmcpye.datarun.mongo.common;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.IdentifiableObjectRepository;
import org.nmcpye.datarun.common.SoftDeleteService;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * @author Hamza Assada 20/03/2025 (7amza.it@gmail.com)
 */
@Slf4j
public abstract class DefaultMongoSoftDeleteService<T extends MongoSoftDeleteObject>
    extends DefaultMongoIdentifiableObjectService<T>
    implements SoftDeleteService<T, String> {


    public DefaultMongoSoftDeleteService(IdentifiableObjectRepository<T, String> repository, CacheManager cacheManager) {
        super(repository, cacheManager);
    }

    @Override
    public T save(T object) {
        log.debug("Request service to save {}:`{}`", getClazz().getSimpleName(), object.getUid());
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

//    @Override
//    protected FilterExpression buildCombinedFilter(QueryRequest queryRequest, String jsonQueryBody) {
//        List<FilterExpression> allFilters = new ArrayList<>();
//
//        // Implicit soft-delete filter
//        if (queryRequest == null || !queryRequest.isIncludeDeleted()) {
//            allFilters.add(new SimpleFilter("deleted", FilterOperator.EQ, false));
//        }
//
//        // v2 JSON expression support
//        if (jsonQueryBody != null && !jsonQueryBody.isEmpty()) {
//            try {
//                FilterExpression parsed = UnifiedQueryParser.parse(jsonQueryBody);
//                allFilters.add(parsed);
//            } catch (Exception e) {
//                throw new IllegalQueryException(ErrorCode.E2050, jsonQueryBody);
//            }
//        }
//
//        // legacy v1 QueryRequest support
//        if (queryRequest != null && queryRequest.getParsedFilter() != null) {
//            allFilters.add(legacyQueryConverter.convert(queryRequest));
//        }
//
//        if (allFilters.isEmpty()) return null;
//        if (allFilters.size() == 1) return allFilters.get(0);
//
//        return new CompoundFilter(LogicalOperator.AND, allFilters);
//    }
}
