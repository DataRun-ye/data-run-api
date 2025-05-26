package org.nmcpye.datarun.common.mongo.impl;

import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.impl.DefaultAuditableObjectService;
import org.nmcpye.datarun.common.mongo.MongoAuditableBaseObject;
import org.nmcpye.datarun.common.repository.AuditableObjectRepository;
import org.nmcpye.datarun.query.MongoQueryBuilder;
import org.nmcpye.datarun.query.UnifiedQueryParser;
import org.nmcpye.datarun.query.filter.FilterExpression;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.GenericQueryService;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

/**
 * @author Hamza Assada, 20/03/2025
 */
public abstract class DefaultMongoAuditableObjectService<T extends MongoAuditableBaseObject>
    extends DefaultAuditableObjectService<T, String>
    implements AuditableObjectService<T, String> {
    @Autowired
    protected MongoQueryBuilder mongoQueryBuilder;

    @Autowired
    protected GenericQueryService queryService;

    public DefaultMongoAuditableObjectService(AuditableObjectRepository<T, String> repository,
                                              CacheManager cacheManager) {
        super(repository, cacheManager);
    }

    @Override
    public Page<T> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        final Query query = new Query();
        if (!SecurityUtils.isSuper()) {
            applySecurityConstraints(query);
        }

//        if (queryRequest == null || !queryRequest.isIncludeDeleted()) {
//            query.addCriteria(Criteria.where("deleted").is(false));
//        }

        if (jsonQueryBody != null && !jsonQueryBody.isEmpty()) {
            try {
                FilterExpression filterExpression = UnifiedQueryParser.parse(jsonQueryBody);
                final var c = mongoQueryBuilder.buildCriteria(List.of(filterExpression));
                query.addCriteria(c);
            } catch (Exception e) {
                throw new IllegalQueryException(ErrorCode.E2050, jsonQueryBody);
            }
        }
        return queryService.query(queryRequest, query, getClazz());
    }

    public void applySecurityConstraints(Query query) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            // No additional constraints for admin
            return;
        }

        String login = SecurityUtils.getCurrentUserLoginOrThrow();
        query.addCriteria(Criteria.where("createdBy").is(login));
    }
}
