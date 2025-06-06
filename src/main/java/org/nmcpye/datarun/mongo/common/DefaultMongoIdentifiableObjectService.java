package org.nmcpye.datarun.mongo.common;

import org.nmcpye.datarun.common.DefaultIdentifiableObjectService;
import org.nmcpye.datarun.common.IdentifiableObjectRepository;
import org.nmcpye.datarun.common.IdentifiableObjectService;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.query.LegacyQueryConverter;
import org.nmcpye.datarun.query.MongoQueryBuilder;
import org.nmcpye.datarun.query.UnifiedQueryParser;
import org.nmcpye.datarun.query.filter.CompoundFilter;
import org.nmcpye.datarun.query.filter.FilterExpression;
import org.nmcpye.datarun.query.filter.LogicalOperator;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada 20/03/2025 <7amza.it@gmail.com>
 */
public abstract class DefaultMongoIdentifiableObjectService<T extends MongoIdentifiableObject>
    extends DefaultIdentifiableObjectService<T, String>
    implements IdentifiableObjectService<T, String> {
    @Autowired
    protected MongoTemplate mongoTemplate;

    @Autowired
    protected MongoQueryBuilder mongoQueryBuilder;

    @Autowired
    protected LegacyQueryConverter legacyQueryConverter;

    public DefaultMongoIdentifiableObjectService(IdentifiableObjectRepository<T, String> repository,
                                                 CacheManager cacheManager) {
        super(repository, cacheManager);
    }

    @Override
    public Page<T> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        final Query query = new Query();
        applySecurityConstraints(query);

        FilterExpression combinedFilter = buildCombinedFilter(queryRequest, jsonQueryBody);
        if (combinedFilter != null) {
            query.addCriteria(mongoQueryBuilder.buildCriteria(combinedFilter));
        }

        return getQueryResult(query, queryRequest.getPageable(), getClazz());
    }


    public Page<T> getQueryResult(Query query, Pageable pageable, Class<T> entityClass) {
        query.with(pageable);

        final Query totalQuery = Query.of(query).limit(-1).skip(-1);

        List<T> results = mongoTemplate.find(query, entityClass);

        Page<T> resultsPage = PageableExecutionUtils.getPage(
            results,
            pageable,
            () -> mongoTemplate.count(totalQuery, entityClass));


        return resultsPage;
    }

    protected void applySecurityConstraints(Query query) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            // No additional constraints for admin
            return;
        }

        String login = SecurityUtils.getCurrentUserLoginOrThrow();
        query.addCriteria(Criteria.where("createdBy").is(login));
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
        if (queryRequest != null && queryRequest.getParsedFilter() != null) {
            allFilters.add(legacyQueryConverter.convert(queryRequest));
        }

        if (allFilters.isEmpty()) return null;
        if (allFilters.size() == 1) return allFilters.get(0);

        return new CompoundFilter(LogicalOperator.AND, allFilters);
    }

//    protected Criteria buildFiltersCriteria(QueryRequest queryRequest, String jsonQueryBody) {
//        Criteria criteria = new Criteria();
//
//        if (queryRequest == null || !queryRequest.isIncludeDeleted()) {
//            criteria.andOperator(Criteria.where("deleted").is(false));
//        }
//
//        if (jsonQueryBody != null && !jsonQueryBody.isEmpty()) {
//            try {
//                FilterExpression filterExpression = UnifiedQueryParser.parse(jsonQueryBody);
//                criteria = mongoQueryBuilder.buildCriteria(List.of(filterExpression));
//            } catch (Exception e) {
//                throw new IllegalQueryException(ErrorCode.E2050, jsonQueryBody);
//            }
//        }
//
//        if (queryRequest != null) {
//            return criteria.andOperator(new RequestQueryBuilder(queryRequest).buildMongoCriteria());
//        }
//        return criteria;
//    }
}
