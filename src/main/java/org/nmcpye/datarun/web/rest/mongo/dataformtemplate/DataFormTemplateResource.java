package org.nmcpye.datarun.web.rest.mongo.dataformtemplate;

import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.service.DataFormTemplateService;
import org.nmcpye.datarun.mongo.service.submissionmigration.DataFormTemplateMigrationService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.utils.FormTemplateSchema;
import org.nmcpye.datarun.web.rest.exception.PathUpdateException;
import org.nmcpye.datarun.web.rest.mongo.AbstractMongoResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

/**
 * REST controller for managing {@link DataForm}.
 */
@RestController
@RequestMapping("/api/custom/dataFormTemplates")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class DataFormTemplateResource extends AbstractMongoResource<DataFormTemplate> {

    final DataFormTemplateService templateService;
    private final FormTemplateSchema formTemplateSchema;
    private final DataFormTemplateMigrationService dataFormTemplateMigrationService;

    public DataFormTemplateResource(DataFormTemplateService templateService,
                                    DataFormTemplateRepository dataFormRepository, FormTemplateSchema formTemplateSchema, DataFormTemplateMigrationService dataFormTemplateMigrationService) {
        super(templateService, dataFormRepository);
        this.templateService = templateService;
        this.formTemplateSchema = formTemplateSchema;
        this.dataFormTemplateMigrationService = dataFormTemplateMigrationService;
    }

    @Override
    protected String getName() {
        return "dataFormTemplates";
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<DataFormTemplate> entities) {
        return super.saveAll(entities);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(DataFormTemplate entity) {
//        var sc = formTemplateSchema.generateSchema(entity);
//        Document document = sc.toDocument();
//        var bson = sc.toDocument().toBsonDocument();
//        var json = document.toJson();
//        var json2 = bson.toJson();
//        log.debug(document.toJson());
//        log.debug(json2);
        return super.saveOne(entity);
    }

//    public void saveEntity(MyEntity entity) {
//        String schema = loadSchemaFromResources(); // Load your schema
//        validateDynamicProperty(entity.getDynamicProperty(), schema);
//        repository.save(entity);
//    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(DataFormTemplate entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<Void> deleteByIdUid(String uid) {
        return super.deleteByIdUid(uid);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<DataFormTemplate> updateEntity(String s, DataFormTemplate entity) throws URISyntaxException {
        return super.updateEntity(s, entity);
    }

    @GetMapping("/migrate")
    public ResponseEntity<String> updatePaths() {
        log.info("REST request to migrate dataFormTemplate elements");

        try {
            dataFormTemplateMigrationService.runForDataFormTemplates();
            return ResponseEntity.ok("Paths updated successfully");
        } catch (Exception e) {
            log.error("Error occurred while updating paths", e);
            throw new PathUpdateException("Failed to update paths", e);
        }
    }
}
