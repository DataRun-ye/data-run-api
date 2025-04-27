package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.common.mongo.impl.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.common.repository.UserRepository;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.service.DataFormTemplateService;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service Implementation for managing {@link DataFormTemplate}.
 */
@Service
@Primary
@Transactional
public class DataFormTemplateServiceImpl
    extends DefaultMongoAuditableObjectService<DataFormTemplate>
    implements DataFormTemplateService {

    public DataFormTemplateServiceImpl(
        DataFormTemplateRepository repository,
        CacheManager cacheManager,
        MongoTemplate mongoTemplate) {
        super(repository, cacheManager, mongoTemplate);
    }

    @CacheEvict(cacheNames = {UserRepository.USER_TEAM_FORM_ACCESS_CACHE,
        UserRepository.USER_ACTIVITY_IDS_CACHE,
        UserRepository.USER_TEAM_IDS_CACHE})
    @Override
    public DataFormTemplate saveWithRelations(DataFormTemplate object) {
        return super.saveWithRelations(object);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DataFormTemplate> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        if (!SecurityUtils.isAuthenticated()) {
            return Page.empty(pageable);
        }

        Query query = new Query();

        final var currentUser = SecurityUtils.getCurrentUserDetailsOrThrow();
        if (!currentUser.isSuper()) {
            query = query.addCriteria(Criteria.where("uid").in(currentUser.getForms()));
        }

        if (!queryRequest.isIncludeDeleted()) {
            query.addCriteria(Criteria.where("deleted").is(false));
        }

        query.with(pageable);

        final Query totalQuery = Query.of(query).limit(-1).skip(-1);

        List<DataFormTemplate> results = mongoTemplate.find(query, DataFormTemplate.class);

        return PageableExecutionUtils.getPage(
            results,
            pageable,
            () -> mongoTemplate.count(totalQuery, DataFormTemplate.class));
    }
}
