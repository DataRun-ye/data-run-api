package org.nmcpye.datarun.web.rest.v1.formtemplate;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.datatemplate.DataTemplateInstanceService;
import org.nmcpye.datarun.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.mongo.dataformtemplate.postsaveprocess.FormTemplateProcessor;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.nmcpye.datarun.web.rest.v1.formtemplate.FormTemplateMergeResource.V1;
import static org.nmcpye.datarun.web.rest.v1.paging.PagingConfigurator.createNextPageLink;
import static org.nmcpye.datarun.web.rest.v1.paging.PagingConfigurator.initPageResponse;

/**
 * REST controller for managing {@link DataForm}.
 */
@RestController
@RequestMapping(value = {V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@Slf4j
public class FormTemplateMergeResource {
    protected static final String NAME = "/dataFormTemplates";
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final FormTemplateProcessor formTemplateProcessor;
    private final DataTemplateInstanceService templateService;

    @Value("${jhipster.clientApp.name}")
    protected String applicationName;

    public FormTemplateMergeResource(FormTemplateProcessor formTemplateProcessor, DataTemplateInstanceService templateService) {
        this.formTemplateProcessor = formTemplateProcessor;
        this.templateService = templateService;
    }


    protected String getName() {
        return "dataFormTemplates";
    }

    /**
     * {@code GET  /Ts} : get all the entities.
     *
     * @param queryRequest the query request parameters.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of assignments in body.
     */
    @GetMapping(value = "")
    protected ResponseEntity<PagedResponse<?>> getAll(QueryRequest queryRequest,
                                                      @RequestBody(required = false) String jsonQueryBody) throws Exception {
        final var userLogin = SecurityUtils.getCurrentUserLoginOrThrow();
        log.debug("REST request to getAll {}:{}", userLogin, getName());

        Page<DataTemplateInstanceDto> processedPage = getList(queryRequest, jsonQueryBody);

        String next = createNextPageLink(processedPage);

        PagedResponse<DataTemplateInstanceDto> response = initPageResponse(processedPage, next, getName());
        return ResponseEntity.ok(response);
    }

    protected Page<DataTemplateInstanceDto> getList(QueryRequest queryRequest, String jsonQueryBody) {
        return templateService.findAllByUser(queryRequest, jsonQueryBody);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @PostMapping("/bulk")
    public ResponseEntity<EntitySaveSummaryVM> saveAll(@Valid @RequestBody List<DataTemplateInstanceDto> entities) {
        log.debug("REST request to saveAll all {}", getName());
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        for (DataTemplateInstanceDto entity : entities) {
            saveEntity(entity, summary);
        }
        return ResponseEntity.ok(summary);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @PostMapping
    public ResponseEntity<EntitySaveSummaryVM> saveOne(@Valid @RequestBody DataTemplateInstanceDto formTemplate) {
        log.debug("REST request to saveOne {}", getName());
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        final var processedTemplate = formTemplateProcessor.processMetadata(
            formTemplateProcessor.validate(formTemplate));
        this.saveEntity((DataTemplateInstanceDto) processedTemplate, summary);

        return ResponseEntity.ok(summary);
    }

    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @PostMapping("/return")
    public ResponseEntity<?> saveReturnSaved(@Valid @RequestBody DataTemplateInstanceDto entity) {
        log.info("Request to save and return {}:{}", entity.getClass().getSimpleName(), entity.getUid());
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        final var saved = saveEntity(entity, summary);

        return ResponseEntity.ok(Objects.requireNonNullElse(saved, summary));
    }

    protected DataTemplateInstanceDto saveEntity(DataTemplateInstanceDto entity, EntitySaveSummaryVM summary) {
        DataTemplateInstanceDto templateVersionDto = null;
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

    @GetMapping("/{id}")
    public ResponseEntity<DataTemplateInstanceDto> getById(@PathVariable("id") String id) {
        log.debug("REST request to get from {}: {}", getName(), id);
        Optional<DataTemplateInstanceDto> entity = templateService.findByUid(id);
        return ResponseUtil.wrapOrNotFound(entity);
    }
}
