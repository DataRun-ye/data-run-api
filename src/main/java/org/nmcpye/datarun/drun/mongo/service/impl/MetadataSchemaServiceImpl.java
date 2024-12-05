package org.nmcpye.datarun.drun.mongo.service.impl;

import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.mongo.domain.DataForm;
import org.nmcpye.datarun.drun.mongo.domain.MetadataSchema;
import org.nmcpye.datarun.drun.mongo.domain.datafield.AbstractField;
import org.nmcpye.datarun.drun.mongo.domain.datafield.ResourceField;
import org.nmcpye.datarun.drun.mongo.domain.datafield.Section;
import org.nmcpye.datarun.drun.mongo.repository.DataFormRepository;
import org.nmcpye.datarun.drun.mongo.repository.MetadataSchemaRepository;
import org.nmcpye.datarun.drun.mongo.service.MetadataSchemaService;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service Implementation for managing {@link DataForm}.
 */
@Service
@Primary
@Transactional
public class MetadataSchemaServiceImpl
    extends IdentifiableMongoServiceImpl<MetadataSchema>
    implements MetadataSchemaService {

    private final Logger log = LoggerFactory.getLogger(MetadataSchemaServiceImpl.class);

    private final MetadataSchemaRepository repositoryCustom;
    private final DataFormRepository dataFormRepository;
    private final ActivityRelationalRepositoryCustom activityRepository;

    private final AssignmentRelationalRepositoryCustom assignmentRepository;
    private final TeamRelationalRepositoryCustom teamRepository;

    private final OrgUnitRelationalRepositoryCustom orgUnitRepository;

    public MetadataSchemaServiceImpl(MetadataSchemaRepository repositoryCustom,
                                     DataFormRepository dataFormRepository,
                                     ActivityRelationalRepositoryCustom activityRepository,
                                     AssignmentRelationalRepositoryCustom assignmentRepository,
                                     TeamRelationalRepositoryCustom teamRepository,
                                     OrgUnitRelationalRepositoryCustom orgUnitRepository) {
        super(repositoryCustom);
        this.repositoryCustom = repositoryCustom;
        this.dataFormRepository = dataFormRepository;
        this.activityRepository = activityRepository;
        this.assignmentRepository = assignmentRepository;
        this.teamRepository = teamRepository;
        this.orgUnitRepository = orgUnitRepository;
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

    @Override
    public MetadataSchema saveWithRelations(MetadataSchema newSubmission) {
        processFields(newSubmission.getFields(), "");
        newSubmission.updateFlattenedFields();
        return repository.save(newSubmission);
    }

    @Override
    public MetadataSchema save(MetadataSchema object) {
        final Integer version =
            Objects.requireNonNullElse(object.getVersion(), 0) + 1;
        object.setVersion(version);
        return super.save(object);
    }

    @Override
    public MetadataSchema update(MetadataSchema object) {
        final Integer version =
            Objects.requireNonNullElse(object.getVersion(), 0) + 1;
        object.setVersion(version);
        return super.update(object);
    }

    @Override
    public Page<MetadataSchema> findAllByUser(Pageable pageable) {
        // If the current user is an admin, fetch all schemas
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repositoryCustom.findAll(pageable);
        }

        // Get all active assignments for the user
        List<Assignment> assignments = assignmentRepository.findAllByStatusUser(false);

        // Get a list of distinct activity UIDs
        List<String> activityUids = assignments.stream()
            .map(Assignment::getActivity)
            .map(Activity::getUid)
            .distinct()
            .toList();

//        List<ResourceField> typeReferenceFields = activityUids.stream()
//            .flatMap(uid -> dataFormRepository.findAllByActivity(uid).stream())
//            .flatMap(form -> form.getFlattenedFields().stream())
//            .filter(AbstractField::isResourceTypeField)
//            .map(ResourceField.class::cast)
//            .toList();
        // Collect referenced metadata schemas from fields
        List<MetadataSchema> schemas = activityUids.stream()
            .flatMap(uid -> dataFormRepository.findAllByActivity(uid).stream())
            .flatMap(form -> form.getFlattenedFields().stream())
            .filter(AbstractField::isResourceTypeField)
            .map(ResourceField.class::cast)
            .map(ResourceField::getResourceMetadataSchema)
            .distinct()
            .map(repositoryCustom::findByUid)
            .flatMap(Optional::stream) // Unwrap only present values
            .toList();

        // Handle paging manually if pageable is unpaged or out of bounds
        return getPagedSchemas(pageable, schemas);
    }

    // Helper method for paging
    private Page<MetadataSchema> getPagedSchemas(Pageable pageable, List<MetadataSchema> schemas) {
        if (!pageable.isPaged()) {
            return new PageImpl<>(schemas);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), schemas.size());

        if (start >= end) {
            return Page.empty(pageable);
        }

        List<MetadataSchema> sublist = schemas.subList(start, end);
        return new PageImpl<>(sublist, pageable, schemas.size());
    }
}
