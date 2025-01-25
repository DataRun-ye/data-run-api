package org.nmcpye.datarun.mongo.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.repository.MetadataSchemaRepository;
import org.nmcpye.datarun.mongo.service.DataFormTemplateService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link DataForm}.
 */
@Service
@Primary
@Transactional
public class DataFormTemplateServiceImpl
    extends IdentifiableMongoServiceImpl<DataFormTemplate>
    implements DataFormTemplateService {

    private final Logger log = LoggerFactory.getLogger(DataFormTemplateServiceImpl.class);

    private final DataFormTemplateRepository repository;

    private final MetadataSchemaRepository metadataSchemaRepository;

    private final TeamRelationalRepositoryCustom teamRepository;

    public DataFormTemplateServiceImpl(
        DataFormTemplateRepository repository,
        MetadataSchemaRepository metadataSchemaRepository,
        MongoTemplate mongoTemplate,
        TeamRelationalRepositoryCustom teamRepository) {
        super(repository, mongoTemplate);
        this.repository = repository;
        this.metadataSchemaRepository = metadataSchemaRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public DataFormTemplate saveWithRelations(DataFormTemplate formTemplate) {
        formTemplate.getFields().stream().filter(f -> f.getType().isReference())
            .forEach(f -> metadataSchemaRepository.findByUid(f.getResourceMetadataSchema()).orElseThrow(() -> {
                log.error("Form's Reference field: {}, is referencing non existing metadata schema", f.getResourceMetadataSchema());
                return new PropertyNotFoundException("Form's Reference field: " +
                    f.getResourceMetadataSchema() + " is referencing non existing metadata schema: ");
            }));


        final Integer version =
            Objects.requireNonNullElse(formTemplate.getVersion(), 0) + 1;
        formTemplate.setVersion(version);
        return repository.save(formTemplate);
    }

    @Override
    public DataFormTemplate update(DataFormTemplate object) {
        final Integer version =
            Objects.requireNonNullElse(repository
                .findByUid(object.getUid()).orElseThrow()
                .getVersion(), 0) + 1;
        object.setVersion(version);

        return super.update(object);
    }

    @Override
    public List<DataFormTemplate> findTemplatesByFieldType(ValueType type) {
        // Create query criteria
        Criteria criteria = Criteria.where("fields.type").is(type);
        Query query = new Query(criteria);

        // Execute query
        return mongoTemplate.find(query, DataFormTemplate.class);
    }

    @Override
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
            () -> mongoTemplate.count(totalQuery, DataForm.class));

        return resultsPage;
    }
}
