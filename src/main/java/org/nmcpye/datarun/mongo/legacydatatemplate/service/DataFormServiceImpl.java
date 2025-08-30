//package org.nmcpye.datarun.mongo.legacydatatemplate.service;
//
//import org.nmcpye.datarun.common.enumeration.FormPermission;
//import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
//import org.nmcpye.datarun.common.feedback.ErrorCode;
//import org.nmcpye.datarun.common.feedback.ErrorMessage;
//import org.nmcpye.datarun.jpa.team.Team;
//import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
//import org.nmcpye.datarun.mongo.common.DefaultMongoIdentifiableObjectService;
//import org.nmcpye.datarun.mongo.domain.DataForm;
//import org.nmcpye.datarun.mongo.domain.datafield.AbstractField;
//import org.nmcpye.datarun.mongo.domain.datafield.ReferenceField;
//import org.nmcpye.datarun.mongo.domain.datafield.Section;
//import org.nmcpye.datarun.mongo.legacydatatemplate.repository.DataFormRepository;
//import org.nmcpye.datarun.mongo.metadataschema.repository.MetadataSchemaRepository;
//import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
//import org.springframework.cache.CacheManager;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
///**
// * Service Implementation for managing {@link DataForm}.
// */
//@Service
//@Primary
//@Transactional
//public class DataFormServiceImpl
//    extends DefaultMongoIdentifiableObjectService<DataForm>
//    implements DataFormService {
//
//    private final DataFormRepository repositoryCustom;
//
//    private final MetadataSchemaRepository metadataSchemaRepository;
//
//    private final TeamRepository teamRepository;
//
//
//    public DataFormServiceImpl(DataFormRepository repositoryCustom,
//                               MetadataSchemaRepository metadataSchemaRepository,
//                               CacheManager cacheManager,
//                               TeamRepository teamRepository) {
//        super(repositoryCustom, cacheManager);
//        this.repositoryCustom = repositoryCustom;
//        this.metadataSchemaRepository = metadataSchemaRepository;
//        this.teamRepository = teamRepository;
//    }
//
//    public <T extends AbstractField> void processFields(List<T> fields, String parentPath, DataForm dataForm) {
//        for (AbstractField field : fields) {
//            String currentPath = parentPath.isEmpty() ? field.getName() : parentPath + AbstractField.PATH_SEP + field.getName();
//            field.setPath(currentPath);
//            if (field instanceof ReferenceField referenceField) {
//                if (referenceField.getResourceType() == null || referenceField.getResourceMetadataSchema() == null) {
//                    throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1102, dataForm.getUid(), field.getName()));
//                }
//
//                if (referenceField.getResourceMetadataSchema() == null) {
//                    throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1103, dataForm.getUid(), field.getName()));
//                }
//
//                if (metadataSchemaRepository.findByUid(referenceField.getResourceMetadataSchema()).isEmpty()) {
//                    throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1104, dataForm.getUid(),
//                        field.getName(),
//                        referenceField.getResourceMetadataSchema()));
//                }
//
//            }
//            // Recursively process nested sections
//            if (field instanceof Section section && section.getFields() != null) {
//                processFields(section.getFields(), currentPath, dataForm);
//            }
//        }
//    }
//
////    private Integer createOrUpdateVersion(DataForm object) {
////        return repositoryCustom
////            .findByUid(object.getUid()).map(DataForm::getVersion).orElse(0);
////    }
//
//    @Override
//    public void preSaveHook(DataForm dataForm) {
//        processFields(dataForm.getFields(), "", dataForm);
//        dataForm.updateFlattenedFields();
////        dataForm.setVersion(createOrUpdateVersion(dataForm) + 1);
//    }
//
//    @Override
//    public Page<DataForm> getAccessibleForms(Pageable pageable) {
//        // Fetch form template IDs for which the team has the specified permission
//        List<String> userTeams = teamRepository
//            .findAllWithEagerRelation()
//            .stream().map(Team::getId)
//            .toList();
//
//        Set<String> formTemplateIds = teamRepository
//            .findAllWithEagerRelation().stream().flatMap((team) -> team.getFormsWithPermission(
//                FormPermission.ADD_SUBMISSIONS).stream()).collect(Collectors.toSet());
//
//        var dataForms = repositoryCustom.findAllByUidIn(formTemplateIds.stream().toList());
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
//        List<DataForm> sublist = dataForms.subList(start, end);
//        return new PageImpl<>(sublist, pageable, dataForms.size());
//    }
//
//
//    @Override
//    public Page<DataForm> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
//
////        Pageable pageable = queryRequest.getPageable();
////
////        if (!SecurityUtils.isAuthenticated()) {
////            return Page.empty(pageable);
////        }
////
////        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
////            return repositoryCustom.findAll(pageable);
////        }
////
////        Set<String> userForms = teamRepository
////            .findAllWithEagerRelation().stream().flatMap((team) -> team.getFormsWithPermission(
////                FormPermission.ADD_SUBMISSIONS).stream()).collect(Collectors.toSet());
////
////        Query query = new Query();
////        query = query.addCriteria(Criteria.where("uid").in(userForms));
////
////        if (!queryRequest.isIncludeDeleted()) {
////            query.addCriteria(Criteria.where("deleted").is(false));
////        }
////
////        query.with(pageable);
////
////        final Query totalQuery = Query.of(query).limit(-1).skip(-1);
////
////        List<DataForm> results = mongoTemplate.find(query, DataForm.class);
////
////        Page<DataForm> resultsPage = PageableExecutionUtils.getPage(
////            results,
////            pageable,
////            () -> mongoTemplate.count(totalQuery, DataForm.class));
//
//        return Page.empty();
//    }
//}
