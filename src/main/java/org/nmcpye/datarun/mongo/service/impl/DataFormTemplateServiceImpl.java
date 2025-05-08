package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.mongo.impl.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.common.repository.UserRepository;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;
import org.nmcpye.datarun.drun.postgres.repository.TeamRepository;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.service.DataFormTemplateService;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.drun.postgres.common.TeamSpecifications.getAssignedSpecification;

/**
 * Service Implementation for managing {@link DataFormTemplate}.
 */
@Service
@Primary
@Transactional
public class DataFormTemplateServiceImpl
    extends DefaultMongoAuditableObjectService<DataFormTemplate>
    implements DataFormTemplateService {
    private final TeamRepository teamRepository;

    public DataFormTemplateServiceImpl(
        DataFormTemplateRepository repository,
        CacheManager cacheManager,
        MongoTemplate mongoTemplate, TeamRepository teamRepository) {
        super(repository, cacheManager, mongoTemplate);
        this.teamRepository = teamRepository;
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
    public Page<DataFormTemplate> findAllByUser(QueryRequest queryRequest) {
        if (!SecurityUtils.isAuthenticated()) {
            return Page.empty(queryRequest.getPageable());
        }

        final var assignedTeamSec = getAssignedSpecification(SecurityUtils
                .getCurrentUserDetails().orElseThrow(() ->
                    new IllegalQueryException(new ErrorMessage(ErrorCode.E3004, getClass().getName()))),
            queryRequest);
        Set<String> userForms = teamRepository
            .findAll(assignedTeamSec).stream().flatMap((team) -> team.getFormsWithPermission(
                FormPermission.ADD_SUBMISSIONS).stream()).collect(Collectors.toSet());

        Query query = new Query();

        final var currentUser = SecurityUtils.getCurrentUserDetailsOrThrow();
//        if (!currentUser.isSuper()) {
//            query = query.addCriteria(Criteria.where("uid").in(currentUser.getUserFormsUIDs()));
//        }
        if (!currentUser.isSuper()) {
            query = query.addCriteria(Criteria.where("uid").in(userForms));
        }

        if (!queryRequest.isIncludeDeleted()) {
            query.addCriteria(Criteria.where("deleted").is(false));
        }

        query.with(queryRequest.getPageable());

        final Query totalQuery = Query.of(query).limit(-1).skip(-1);

        List<DataFormTemplate> results = mongoTemplate.find(query, DataFormTemplate.class);

        return PageableExecutionUtils.getPage(
            results,
            queryRequest.getPageable(),
            () -> mongoTemplate.count(totalQuery, DataFormTemplate.class));
    }
}
