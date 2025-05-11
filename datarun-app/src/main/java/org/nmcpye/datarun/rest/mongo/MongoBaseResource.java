package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.repository.AuditableObjectRepository;
import org.nmcpye.datarun.query.MongoQueryBuilder;
import org.nmcpye.datarun.query.UnifiedQueryParser;
import org.nmcpye.datarun.query.filter.FilterExpression;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.common.BaseReadWriteResource;
import org.nmcpye.datarun.web.rest.mongo.submission.GenericQueryService;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

//@RequestMapping("ApiVersion.API_CUSTOM, ApiVersion.API_V1")
public abstract class MongoBaseResource<T extends AuditableObject<String>>
    extends BaseReadWriteResource<T, String> {

    @Autowired
    protected MongoQueryBuilder mongoQueryBuilder;

    @Autowired
    protected GenericQueryService queryService;

    protected MongoBaseResource(AuditableObjectService<T, String> identifiableService,
                                AuditableObjectRepository<T, String> repository) {
        super(identifiableService, repository);
    }

    //    public Query applySecurityConstraints(Query query) {
//        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
//            return query;
//        }
//
//        final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();
//
//        final String currentUser = SecurityUtils.getCurrentUserLoginOrThrow();
//
//        query.addCriteria(Criteria.where("createdBy").is(currentUser));
//
//        return query;
//    }
//
    protected void applySecurityConstraints(Query query) {
    }

    @Override
    protected MongoQueryBuilder getQueryBuilder() {
        return mongoQueryBuilder;
    }

    @Override
    protected Page<T> getList(QueryRequest queryRequest, String jsonQueryBody) {
        final Query query = new Query();
        if (!SecurityUtils.isSuper()) {
            applySecurityConstraints(query);
        }

        if (!queryRequest.isIncludeDeleted()) {
            query.addCriteria(Criteria.where("deleted").is(false));
        }

        if (jsonQueryBody != null && !jsonQueryBody.isEmpty()) {
            try {
                FilterExpression filterExpression = UnifiedQueryParser.parse(jsonQueryBody);
                query.addCriteria(mongoQueryBuilder.buildCriteria(List.of(filterExpression)));
            } catch (Exception e) {
                throw new IllegalQueryException(ErrorCode.E2050, jsonQueryBody);
            }
        }
        return queryService.query(queryRequest, query, getEntityClass());
//        return super.getList(queryRequest, jsonQueryBody);
    }
}
