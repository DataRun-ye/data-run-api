package org.nmcpye.datarun.web.rest.mongo.submission;

import org.nmcpye.datarun.query.MongoQueryBuilder;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenericQueryService {

    private final MongoTemplate mongoTemplate;
    private final MongoQueryBuilder mongoQueryBuilder;

    public GenericQueryService(MongoTemplate mongoTemplate, MongoQueryBuilder mongoQueryBuilder) {
        this.mongoTemplate = mongoTemplate;
        this.mongoQueryBuilder = mongoQueryBuilder;
    }

    @Deprecated(since = "api v1")
    public <T> Page<T> query(QueryRequest request, Class<T> entityClass) {
        Query query = new RequestQueryBuilder(request).buildForMongo(new Query());

        if (!request.isIncludeDeleted()) {
            query.addCriteria(Criteria.where("deleted").is(false));
        }

        Pageable pageable = request.getPageable();
        query.with(pageable);

        final Query totalQuery = Query.of(query).limit(-1).skip(-1);

        List<T> results = mongoTemplate.find(query, entityClass);

        Page<T> resultsPage = PageableExecutionUtils.getPage(
            results,
            pageable,
            () -> mongoTemplate.count(totalQuery, entityClass));


        return resultsPage;
    }

    public <T> Page<T> query(QueryRequest request,
                             Query query, Class<T> entityClass) {
        query = new RequestQueryBuilder(request).buildForMongo(query);

        Pageable pageable = request.getPageable();
        query.with(pageable);

        final Query totalQuery = Query.of(query).limit(-1).skip(-1);

        List<T> results = mongoTemplate.find(query, entityClass);

        Page<T> resultsPage = PageableExecutionUtils.getPage(
            results,
            pageable,
            () -> mongoTemplate.count(totalQuery, entityClass));


        return resultsPage;
    }

    public Query applySecurityConstraints(Query query) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return query;
        }

        final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();

        final String currentUser = SecurityUtils.getCurrentUserLoginOrThrow();

        query.addCriteria(Criteria.where("createdBy").is(currentUser));

        return query;
    }
}
