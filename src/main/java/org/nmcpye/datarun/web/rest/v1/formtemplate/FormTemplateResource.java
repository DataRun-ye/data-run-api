package org.nmcpye.datarun.web.rest.v1.formtemplate;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.mapper.dto.FormTemplateVersionDto;
import org.nmcpye.datarun.mapper.dto.SaveFormTemplateDto;
import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.mongo.service.FormTemplateService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.mongo.dataformtemplate.postsaveprocess.FormTemplateProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;

import java.util.List;
import java.util.Objects;

import static org.nmcpye.datarun.web.rest.v1.formtemplate.FormTemplateResource.V1;

/**
 * REST controller for managing {@link DataForm}.
 */
@RestController
@RequestMapping(value = {V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@Slf4j
public class FormTemplateResource {
    protected static final String NAME = "/dataFormTemplates";
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final FormTemplateProcessor formTemplateProcessor;
    private final FormTemplateService templateService;

    @Value("${jhipster.clientApp.name}")
    protected String applicationName;

    public FormTemplateResource(FormTemplateProcessor formTemplateProcessor, FormTemplateService templateService) {
        this.formTemplateProcessor = formTemplateProcessor;
        this.templateService = templateService;
    }


    protected String getName() {
        return "dataFormTemplates";
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @PostMapping("/bulk")
    public ResponseEntity<EntitySaveSummaryVM> saveAll(@Valid @RequestBody List<SaveFormTemplateDto> entities) {
        log.debug("REST request to saveAll all {}", getName());
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        for (SaveFormTemplateDto entity : entities) {
            saveEntity(entity, summary);
        }
        return ResponseEntity.ok(summary);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<EntitySaveSummaryVM> saveOne(SaveFormTemplateDto formTemplate) {
        log.debug("REST request to saveOne {}", getName());
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        final var processedTemplate = formTemplateProcessor.processMetadata(
            formTemplateProcessor.validate(formTemplate));
        this.saveEntity((SaveFormTemplateDto) processedTemplate, summary);

        return ResponseEntity.ok(summary);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<?> saveReturnSaved(@Valid @RequestBody SaveFormTemplateDto entity) {
        log.info("Request to save and return {}:{}", entity.getClass().getSimpleName(), entity.getUid());
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        final var saved = saveEntity(entity, summary);

        return ResponseEntity.ok(Objects.requireNonNullElse(saved, summary));
    }

    protected FormTemplateVersionDto saveEntity(SaveFormTemplateDto entity, EntitySaveSummaryVM summary) {
        FormTemplateVersionDto templateVersionDto = null;
        try {
            if (entity.getUid() != null && templateService.existsByUid(entity.getUid())) {
                templateVersionDto = templateService.update(entity);
                summary.getUpdated().add(templateVersionDto.getUid());
            } else {
                templateVersionDto = templateService.save(entity);
                summary.getCreated().add(templateVersionDto.getUid());
            }
        } catch (Exception e) {
            log.error("REST Error Saving entity {}:{}", "FormTemplate", entity.getUid());
            summary.getFailed().put(entity.getUid(), e.getMessage());
            throw new IllegalQueryException(e.getMessage());
        }
        return templateVersionDto;
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteByIdUid(@PathVariable("id") String id) {
        log.debug("REST request to delete from {}: {}", getName(), id);
        templateService.deleteByUid(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil
                .createEntityDeletionAlert(applicationName, true, getName(), id)).build();
    }
}
