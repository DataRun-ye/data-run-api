package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.common.mongo.impl.DefaultMongoIdentifiableService;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;
import org.nmcpye.datarun.drun.postgres.repository.TeamRepository;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.service.DataFormTemplateService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link DataFormTemplate}.
 */
@Service
@Primary
@Transactional
public class DataFormTemplateServiceImpl
    extends DefaultMongoIdentifiableService<DataFormTemplate>
    implements DataFormTemplateService {

    private final DataFormTemplateRepository repository;

    private final TeamRepository teamRepository;


    public DataFormTemplateServiceImpl(
        DataFormTemplateRepository repository,
        CacheManager cacheManager,
        MongoTemplate mongoTemplate,
        TeamRepository teamRepository) {
        super(repository, cacheManager, mongoTemplate);
        this.repository = repository;
        this.teamRepository = teamRepository;
    }

    @Override
    public DataFormTemplate saveWithRelations(DataFormTemplate formTemplate) {
//        final var sections = formElementConfigService.configureSectionsPath(formTemplate.getSections());
//        formTemplate.setSections(sections);
//        final var fields = formElementConfigService
//            .configureAndValidateFields(formTemplate.getFields(), sections);
//        formTemplate.version(createOrUpdateVersion(formTemplate) + 1);
//        formTemplate.setFields(fields);
        return repository.save(formTemplate);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DataFormTemplate> findAllByUser(Pageable pageable, QueryRequest queryRequest) {

        if (!SecurityUtils.isAuthenticated()) {
            return Page.empty(pageable);
        }

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(pageable);
        }


        Set<String> userForms = teamRepository
            .findAllWithEagerRelation().stream().flatMap((team) -> team.getFormsWithPermission(
                FormPermission.ADD_SUBMISSIONS).stream()).collect(Collectors.toSet());

        Query query = new Query();
        query = query.addCriteria(Criteria.where("uid").in(userForms));

        if (!queryRequest.isIncludeDeleted()) {
            query.addCriteria(Criteria.where("deleted").is(false));
        }

        query.with(pageable);

        final Query totalQuery = Query.of(query).limit(-1).skip(-1);

        List<DataFormTemplate> results = mongoTemplate.find(query, DataFormTemplate.class);

        Page<DataFormTemplate> resultsPage = PageableExecutionUtils.getPage(
            results,
            pageable,
            () -> mongoTemplate.count(totalQuery, DataFormTemplate.class));

//        if (queryRequest.isMergeElements()) {
//            resultsPage.forEach(formElementConfigService::mergeFormElements);
//        }

        return resultsPage;
    }
}
