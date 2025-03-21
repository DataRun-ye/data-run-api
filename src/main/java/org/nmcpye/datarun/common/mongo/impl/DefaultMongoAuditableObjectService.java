package org.nmcpye.datarun.common.mongo.impl;

import org.nmcpye.datarun.common.mongo.MongoAuditableBaseObject;
import org.nmcpye.datarun.common.repository.AuditableObjectRepository;
import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.common.impl.DefaultAuditableObjectService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.MongoQueryBuilder;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

/**
 * @author Hamza, 20/03/2025
 */
public abstract class DefaultMongoAuditableObjectService<T extends MongoAuditableBaseObject>
    extends DefaultAuditableObjectService<T, String>
    implements AuditableObjectService<T, String> {
    final protected MongoTemplate mongoTemplate;

    public DefaultMongoAuditableObjectService(AuditableObjectRepository<T, String> repository,
                                              CacheManager cacheManager, MongoTemplate mongoTemplate) {
        super(repository, cacheManager);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<T> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        return query(queryRequest);
    }

    @Override
    public List<T> findAllByUser(QueryRequest queryRequest) {
        Query query = new Query();
        return getFormSubmissions(query, queryRequest);
    }

    public Page<T> query(QueryRequest request) {
        Query query = MongoQueryBuilder.buildQuery(request.parseFilters());
        query = MongoQueryBuilder.addProjections(query, request.getFields());

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        query.with(pageable);

        final Query totalQuery = Query.of(query).limit(-1).skip(-1);

        List<T> results = mongoTemplate.find(query, getClazz());

        return PageableExecutionUtils.getPage(
            results,
            pageable,
            () -> mongoTemplate.count(totalQuery, getClazz()));
    }

    protected List<T> getFormSubmissions(Query query, QueryRequest queryRequest) {
        applySecurityConstraints(query);
        if (!queryRequest.isIncludeDeleted()) {
            query.addCriteria(Criteria.where("deleted").is(false));
        }
        return mongoTemplate.find(query, getClazz());
    }

    private static void applySecurityConstraints(Query query) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            // No additional constraints for admin
            return;
        }

        String login = SecurityUtils.getCurrentUserLoginOrThrow();
        query.addCriteria(Criteria.where("createdBy").is(login));
    }
}
