package org.nmcpye.datarun.mongo.metadataschema.service;

import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateInstanceService;
import org.nmcpye.datarun.mongo.common.DefaultMongoIdentifiableObjectService;
import org.nmcpye.datarun.mongo.domain.MetadataSchema;
import org.nmcpye.datarun.mongo.domain.datafield.AbstractField;
import org.nmcpye.datarun.mongo.domain.datafield.Section;
import org.nmcpye.datarun.mongo.metadataschema.repository.MetadataSchemaRepository;
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
    extends DefaultMongoIdentifiableObjectService<MetadataSchema>
    implements MetadataSchemaService {

    private final MetadataSchemaRepository repository;
    private final DataTemplateInstanceService templateInstanceService;

    private final AssignmentRepository flowInstanceRepository;

    public MetadataSchemaServiceImpl(MetadataSchemaRepository repository,
                                     CacheManager cacheManager,
                                     DataTemplateInstanceService templateInstanceService,
                                     AssignmentRepository flowInstanceRepository) {
        super(repository, cacheManager);
        this.repository = repository;
        this.templateInstanceService = templateInstanceService;
        this.flowInstanceRepository = flowInstanceRepository;
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
    public void preSaveHook(MetadataSchema newSubmission) {
        processFields(newSubmission.getFields(), "");
        newSubmission.updateFlattenedFields();
        newSubmission.setVersion(createOrUpdateVersion(newSubmission) + 1);
    }
}
