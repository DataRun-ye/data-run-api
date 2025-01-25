//package org.nmcpye.datarun.mongo.service.impl;
//
//import org.nmcpye.datarun.drun.postgres.domain.Team;
//import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;
//import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
//import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
//import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
//import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
//import org.nmcpye.datarun.mongo.domain.DataForm;
//import org.nmcpye.datarun.mongo.domain.DataFormTemplateMaster;
//import org.nmcpye.datarun.mongo.domain.DataFormTemplateVersion;
//import org.nmcpye.datarun.mongo.domain.datafield.AbstractField;
//import org.nmcpye.datarun.mongo.domain.datafield.ReferenceField;
//import org.nmcpye.datarun.mongo.domain.datafield.Section;
//import org.nmcpye.datarun.mongo.repository.DataFormRepository;
//import org.nmcpye.datarun.mongo.repository.DataFormTemplateMasterRepository;
//import org.nmcpye.datarun.mongo.repository.DataFormTemplateVersionRepository;
//import org.nmcpye.datarun.mongo.repository.MetadataSchemaRepository;
//import org.nmcpye.datarun.mongo.service.DataFormTemplateMasterService;
//import org.nmcpye.datarun.repository.UserRepository;
//import org.nmcpye.datarun.security.AuthoritiesConstants;
//import org.nmcpye.datarun.security.SecurityUtils;
//import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.support.PageableExecutionUtils;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.Objects;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * Service Implementation for managing {@link DataForm}.
// */
//@Service
//@Primary
//@Transactional
//public class DataFormTemplateMasterServiceImpl
//    extends IdentifiableMongoServiceImpl<DataFormTemplateMaster>
//    implements DataFormTemplateMasterService {
//
//    private final Logger log = LoggerFactory.getLogger(DataFormTemplateMasterServiceImpl.class);
//
//    private final DataFormRepository formRepository;
//
//    private final DataFormTemplateMasterRepository repository;
//
//    private final DataFormTemplateVersionRepository templateVersionRepository;
//
//    private final MetadataSchemaRepository metadataSchemaRepository;
//
//    private final ActivityRelationalRepositoryCustom activityRepository;
//
//    private final AssignmentRelationalRepositoryCustom assignmentRepository;
//
//    private final OrgUnitRelationalRepositoryCustom orgUnitRepository;
//
//    private final TeamRelationalRepositoryCustom teamRepository;
//
//    private final UserRepository userRepository;
//
//    public DataFormTemplateMasterServiceImpl(DataFormRepository formRepository,
//                                             DataFormTemplateMasterRepository repository, DataFormTemplateVersionRepository templateVersionRepository,
//                                             MetadataSchemaRepository metadataSchemaRepository,
//                                             ActivityRelationalRepositoryCustom activityRepository,
//                                             AssignmentRelationalRepositoryCustom assignmentRepository,
//                                             OrgUnitRelationalRepositoryCustom orgUnitRepository,
//                                             MongoTemplate mongoTemplate,
//                                             UserRepository userRepository,
//                                             TeamRelationalRepositoryCustom teamRepository) {
//        super(repository, mongoTemplate);
//        this.formRepository = formRepository;
//        this.repository = repository;
//        this.templateVersionRepository = templateVersionRepository;
//        this.metadataSchemaRepository = metadataSchemaRepository;
//        this.activityRepository = activityRepository;
//        this.assignmentRepository = assignmentRepository;
//        this.orgUnitRepository = orgUnitRepository;
//        this.teamRepository = teamRepository;
//        this.userRepository = userRepository;
//    }
//
//    public <T extends AbstractField> void processFields(List<T> fields, String parentPath) {
//        for (AbstractField field : fields) {
//            String currentPath = parentPath.isEmpty() ? field.getName() : parentPath + AbstractField.PATH_SEP + field.getName();
//            field.setPath(currentPath);
//            if (field instanceof ReferenceField referenceField) {
//                if (referenceField.getResourceType() == null || referenceField.getResourceMetadataSchema() == null) {
//                    throw new IllegalArgumentException(field.getName() + ": is of Reference type but does not specify Resource Type [OrgUnit, Team, Activity...etc] or ResourceMetadataSchema (The form used to submit the metadata of the reference type)");
//                }
//
//                if (metadataSchemaRepository.findByUid(referenceField.getResourceMetadataSchema()).isEmpty()) {
//                    throw new IllegalArgumentException("Field: " + field.getName() + ": Specified ResourceMetadataSchema " + referenceField.getResourceMetadataSchema() + " does not exist");
//                }
//
//            }
//            // Recursively process nested sections
//            if (field instanceof Section section && section.getFields() != null) {
//                processFields(section.getFields(), currentPath);
//            }
//        }
//    }
//
//    @Override
//    public DataFormTemplateMaster saveWithRelations(DataFormTemplateMaster dataForm) {
//
//        return repository.save(dataForm);
//    }
//
//    @Override
//    public DataFormTemplateMaster save(DataFormTemplateMaster object) {
//        final Integer version =
//            Objects.requireNonNullElse(object.getVersion(), 0) + 1;
//        object.setVersion(version);
//        return super.save(object);
//    }
//
//    @Override
//    public DataFormTemplateMaster update(DataFormTemplateMaster object) {
////        final Integer version =
////            Objects.requireNonNullElse(repository
////                .findByUid(object.getUid())
////                .get().getVersion(), 0) + 1;
////        object.setVersion(version);
////        createNewVersion(object.getUid(), )
//
//        return super.update(object);
//    }
//
//    @Override
//    public Page<DataFormTemplateMaster> getAccessibleForms(Pageable pageable) {
//        // Fetch form template IDs for which the team has the specified permission
//        List<Long> userTeams = teamRepository
//            .findAllWithEagerRelation()
//            .stream().map(Team::getId)
//            .toList();
//
//        Set<String> formTemplateIds = teamRepository
//            .findAllWithEagerRelation().stream().flatMap((team) -> team.getFormsWithPermission(
//                FormPermission.ADD_SUBMISSIONS).stream()).collect(Collectors.toSet());
//
//        var dataForms = repository.findAllByUidIn(formTemplateIds.stream().toList()).stream().toList();
//
//        if (!pageable.isPaged()) {
//            return new PageImpl<>(dataForms);
//        }
//
//        int start = (int) pageable.getOffset();
//        int end = Math.min((start + pageable.getPageSize()), dataForms.size());
//        if (start > end) {
//            return Page.empty(pageable);
//        }
//
//        List<DataFormTemplateMaster> sublist = dataForms.subList(start, end);
//        return new PageImpl<>(sublist, pageable, dataForms.size());
//    }
//
//
//    @Override
//    public Page<DataFormTemplateMaster> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
//
//        if (!SecurityUtils.isAuthenticated()) {
//            return Page.empty(pageable);
//        }
//
//        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
//            return repository.findAll(pageable);
//        }
//
//        List<Team> userTeams = teamRepository
//            .findAllWithEagerRelation();
//
//        Set<String> userForms = teamRepository
//            .findAllWithEagerRelation().stream().flatMap((team) -> team.getFormsWithPermission(
//                FormPermission.ADD_SUBMISSIONS).stream()).collect(Collectors.toSet());
//
//        Query query = new Query();
//        query = query.addCriteria(Criteria.where("uid").in(userForms));
//
//        if (!queryRequest.isIncludeDeleted()) {
//            query.addCriteria(Criteria.where("deleted").is(false));
//        }
//
//        query.with(pageable);
//
//        final Query totalQuery = Query.of(query).limit(-1).skip(-1);
//
//        List<DataFormTemplateMaster> results = mongoTemplate.find(query, DataFormTemplateMaster.class);
//
//        Page<DataFormTemplateMaster> resultsPage = PageableExecutionUtils.getPage(
//            results,
//            pageable,
//            () -> mongoTemplate.count(totalQuery, DataForm.class));
//
//        return resultsPage;
//    }
//
//    public DataFormTemplateVersion createNewVersion(String masterTemplateUid,
//                                                    DataFormTemplateVersion newVersionDetails) {
//        DataFormTemplateMaster masterTemplate = mongoTemplate.findOne(
//            Query.query(Criteria.where("uid").is(masterTemplateUid)),
//            DataFormTemplateMaster.class
//        );
//
//        if (masterTemplate == null) {
//            throw new IllegalArgumentException("Master template not found.");
//        }
//
//        // Retrieve the latest version for master template
//        DataFormTemplateVersion lastVersion = mongoTemplate.findOne(
//            Query.query(Criteria.where("parentUid").is(masterTemplateUid).and("isLatest").is(true)),
//            DataFormTemplateVersion.class
//        );
//
//        // Assign version number and uid
//        int newVersionNumber = (lastVersion != null) ? lastVersion.getVersion() + 1 : 1;
//
//        DataFormTemplateVersion newVersion = new DataFormTemplateVersion();
//        newVersion.setTemplateMasterUid(masterTemplateUid);
//        newVersion.setVersion(newVersionNumber);
//        newVersion.setFieldsConf(newVersionDetails.getFieldsConf());
//        newVersion.setSectionsConf(newVersionDetails.getSectionsConf());
//        newVersion.setLatest(true);
//
//        mongoTemplate.save(newVersion);
//
//        // Update the master template to point to the latest version
//        masterTemplate.setLatestVersionUid(newVersion.getUid());
//        masterTemplate.setLastUpdated(Instant.now());
//        mongoTemplate.save(masterTemplate);
//
//        // Mark the previous version as no longer the latest
//        if (lastVersion != null) {
//            lastVersion.setLatest(false);
//            mongoTemplate.save(lastVersion);
//        }
//
//        return newVersion;
//    }
//
//}
