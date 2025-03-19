package org.nmcpye.datarun.web.rest.mongo.dataformtemplate;

import jakarta.validation.Valid;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.service.DataFormTemplateService;
import org.nmcpye.datarun.mongo.service.submissionmigration.DataFormTemplateMigrationService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.mongo.AbstractMongoResource;
import org.nmcpye.datarun.web.rest.mongo.dataformtemplate.postsaveprocess.FormTemplateProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    private final DataFormTemplateService templateService;
    //    private final FormTemplateSchema formTemplateSchema;
    private final DataFormTemplateMigrationService dataFormTemplateMigrationService;
    private final FormTemplateProcessor formTemplateProcessor;
//    private final DataFormTemplateRepository dataFormTemplateRepository;

    public DataFormTemplateResource(DataFormTemplateService templateService,
                                    DataFormTemplateRepository dataFormRepository,
//                                    FormTemplateSchema formTemplateSchema,
                                    DataFormTemplateMigrationService dataFormTemplateMigrationService, FormTemplateProcessor formTemplateProcessor,
                                    DataFormTemplateRepository dataFormTemplateRepository) {
        super(templateService, dataFormRepository);
        this.templateService = templateService;
        this.dataFormTemplateMigrationService = dataFormTemplateMigrationService;
        this.formTemplateProcessor = formTemplateProcessor;
//        this.dataFormTemplateRepository = dataFormTemplateRepository;
    }

    @Override
    protected String getName() {
        return "dataFormTemplates";
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(@Valid @RequestBody List<DataFormTemplate> templates) {
        log.debug("REST request to saveAll all {}", getName());
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        templates.stream().map(formTemplateProcessor::validateElements)
            .map(formTemplateProcessor::processMetadata).forEach(t -> this.saveEntity(t, summary));

        return ResponseEntity.ok(summary);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(DataFormTemplate formTemplate) {
        log.debug("REST request to saveOne {}", getName());
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        final var processedTemplate = formTemplateProcessor.processMetadata(formTemplateProcessor.validateElements(formTemplate));
        this.saveEntity(processedTemplate, summary);

        return ResponseEntity.ok(summary);
    }

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
            throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1000, e.getMessage(), "Failed to update paths"));
        }
    }
}
