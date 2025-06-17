package org.nmcpye.datarun.web.rest.mongo.dataformtemplate;

import jakarta.validation.Valid;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.legacydatatemplate.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.legacydatatemplate.service.DataFormTemplateService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.mongo.MongoBaseResource;
import org.nmcpye.datarun.web.rest.mongo.dataformtemplate.postsaveprocess.FormTemplateProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

import static org.nmcpye.datarun.web.rest.mongo.dataformtemplate.DataFormTemplateResource.CUSTOM;

/**
 * REST controller for managing {@link DataFormTemplate}.
 */
@RestController
@RequestMapping(value = {CUSTOM})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class DataFormTemplateResource extends MongoBaseResource<DataFormTemplate> {
    protected static final String NAME = "/dataFormTemplates";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;

    private final FormTemplateProcessor formTemplateProcessor;

    public DataFormTemplateResource(DataFormTemplateService templateService,
                                    DataFormTemplateRepository dataFormRepository,
                                    FormTemplateProcessor formTemplateProcessor) {
        super(templateService, dataFormRepository);
        this.formTemplateProcessor = formTemplateProcessor;
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
        templates.stream().map(formTemplateProcessor::validate)
            .map(formTemplateProcessor::processMetadata)
            .forEach(t -> this.saveEntity((DataFormTemplate) t, summary));

        return ResponseEntity.ok(summary);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveOne(DataFormTemplate formTemplate) {
        log.debug("REST request to saveOne {}", getName());
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        final var processedTemplate = formTemplateProcessor.processMetadata(
            formTemplateProcessor.validate(formTemplate));
        this.saveEntity((DataFormTemplate) processedTemplate, summary);

        return ResponseEntity.ok(summary);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<?> saveReturnSaved(DataFormTemplate entity) {
        return super.saveReturnSaved(entity);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<Void> deleteByIdUid(String id) {
        return super.deleteByIdUid(id);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @Override
    public ResponseEntity<DataFormTemplate> updateEntity(String uid, DataFormTemplate entity) throws URISyntaxException {
        return super.updateEntity(uid, entity);
    }
}
