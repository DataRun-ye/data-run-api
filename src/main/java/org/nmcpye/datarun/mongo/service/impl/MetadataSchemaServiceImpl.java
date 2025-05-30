package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.common.mongo.impl.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.mongo.domain.MetadataSchema;
import org.nmcpye.datarun.mongo.domain.datafield.AbstractField;
import org.nmcpye.datarun.mongo.domain.datafield.Section;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.repository.MetadataSchemaRepository;
import org.nmcpye.datarun.mongo.service.MetadataSchemaService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service Implementation for managing {@link MetadataSchema}.
 */
@Service
@Primary
@Transactional
public class MetadataSchemaServiceImpl
    extends DefaultMongoAuditableObjectService<MetadataSchema>
    implements MetadataSchemaService {

    private final MetadataSchemaRepository repository;
    private final DataFormTemplateRepository dataFormRepository;

    private final AssignmentRepository assignmentRepository;

    public MetadataSchemaServiceImpl(MetadataSchemaRepository repository,
                                     CacheManager cacheManager,
                                     DataFormTemplateRepository dataFormRepository,
                                     AssignmentRepository assignmentRepository) {
        super(repository, cacheManager);
        this.repository = repository;
        this.dataFormRepository = dataFormRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public void processFields(List<AbstractField> fields, String parentPath) {
        for (AbstractField field : fields) {
            String currentPath = parentPath.isEmpty() ? field.getName() : parentPath + AbstractField.PATH_SEP + field.getName();
            field.setPath(currentPath);
//            field.setSection(parentPath.isEmpty() ? null : parentPath);

            // Recursively process nested sections
            if (field instanceof Section section && section.getFields() != null) {
                processFields(section.getFields(), currentPath);
            }
        }
    }

    private Integer createOrUpdateVersion(MetadataSchema object) {
        return repository
            .findByUid(object.getUid()).map(MetadataSchema::getVersion).orElse(0);
    }

    @Override
    public MetadataSchema saveWithRelations(MetadataSchema newSubmission) {
        processFields(newSubmission.getFields(), "");
        newSubmission.updateFlattenedFields();
        newSubmission.setVersion(createOrUpdateVersion(newSubmission) + 1);
        return save(newSubmission);
    }
//
//    @Override
//    public Page<MetadataSchema> findAllByUser(QueryRequest queryRequest) {
//        Pageable pageable = queryRequest.getPageable();
//
//        // If the current user is an admin, fetch all schemas
//        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
//            return repository.findAll(pageable);
//        }
//
//        // Get all active assignments for the user
//        List<Assignment> assignments = assignmentRepository.findAllByStatusUser(false);
//
//        // Get a list of distinct activity UIDs
//        List<String> activityUids = assignments.stream()
//            .map(Assignment::getActivity)
//            .map(Activity::getUid)
//            .distinct()
//            .toList();
//
//        // Collect referenced metadata schemas from fields
//        List<MetadataSchema> schemas = activityUids.stream()
//            .flatMap(uid -> dataFormRepository.findAllByActivity(uid).stream())
//            .flatMap(form -> form.getFlattenedFields().stream())
//            .filter(AbstractField::isResourceTypeField)
//            .map(ReferenceField.class::cast)
//            .map(ReferenceField::getResourceMetadataSchema)
//            .distinct()
//            .map(repository::findByUid)
//            .flatMap(Optional::stream) // Unwrap only present values
//            .toList();
//
//        // Handle paging manually if pageable is unpaged or out of bounds
//        return getPagedSchemas(pageable, schemas);
//    }

//    // Helper method for paging
//    private Page<MetadataSchema> getPagedSchemas(Pageable pageable, List<MetadataSchema> schemas) {
//        if (!pageable.isPaged()) {
//            return new PageImpl<>(schemas);
//        }
//
//        int start = (int) pageable.getOffset();
//        int end = Math.min(start + pageable.getPageSize(), schemas.size());
//
//        if (start >= end) {
//            return Page.empty(pageable);
//        }
//
//        List<MetadataSchema> sublist = schemas.subList(start, end);
//        return new PageImpl<>(sublist, pageable, schemas.size());
//    }
}
