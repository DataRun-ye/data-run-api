package org.nmcpye.datarun.web.rest.common;

import jakarta.validation.Valid;
import org.nmcpye.datarun.common.IdentifiableRepository;
import org.nmcpye.datarun.drun.postgres.common.Identifiable;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.web.rest.errors.BadRequestAlertException;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

//@RequestMapping("/api/custom")
public abstract class AbstractResource<T extends Identifiable<ID>, ID extends Serializable> {

    protected final Logger log = LoggerFactory.getLogger(AbstractResource.class);

    @Value("${jhipster.clientApp.name}")
    protected String applicationName;

    final protected IdentifiableService<T, ID> identifiableService;
    final protected IdentifiableRepository<T, ID> repository;

    protected AbstractResource(IdentifiableService<T, ID> identifiableService,
                               IdentifiableRepository<T, ID> repository) {
        this.identifiableService = identifiableService;
        this.repository = repository;
    }

    protected CrudRepository<T, ID> getRepository() {
        return repository;
    }

    /**
     * {@code GET  /Ts} : get all the entities.
     *
     * @param queryRequest the query request parameters.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of assignments in body.
     */
    @GetMapping("")
    protected ResponseEntity<PagedResponse<?>> getAll(
        QueryRequest queryRequest) {
        Pageable pageable = PageRequest.of(queryRequest.getPage(), queryRequest.getSize());

        if (!queryRequest.isPaged()) {
            pageable = Pageable.unpaged();
        }

        Page<T> processedPage = getList(pageable, queryRequest);

        String next = createNextPageLink(processedPage);

        PagedResponse<T> response = initPageResponse(processedPage, next);
        return ResponseEntity.ok(response);
    }

    protected PagedResponse<T> initPageResponse(Page<T> page, String next) {
        PagedResponse<T> response = new PagedResponse<>(page, getName(), next);
        response.setNextPage(next);
        response.setEntityName(getName());
        return response;
    }

    protected String createNextPageLink(Page<?> page) {
        if (page.hasNext()) {
            return ServletUriComponentsBuilder.fromCurrentRequest()
                .queryParam("page", page.getNumber() + 1) // page is 0-based, but we display it 1-based
                .toUriString();
        } else {
            return null;
        }
    }

    private Class<T> entityClass;

    protected final Class<T> getEntityClass() {
        if (entityClass == null) {
            Type[] actualTypeArguments = ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments();
            entityClass = (Class<T>) actualTypeArguments[0];
        }

        return entityClass;
    }

    @PostMapping("/bulk")
    public ResponseEntity<EntitySaveSummaryVM> saveAll(@Valid @RequestBody List<T> entities) {
        log.debug("REST request to saveAll all {}", getName());
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        for (T entity : entities) {
            saveEntity(entity, summary);
        }
        return ResponseEntity.ok(summary);
    }

    @PostMapping
    public ResponseEntity<EntitySaveSummaryVM> saveOne(@Valid @RequestBody T entity) {
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        saveEntity(entity, summary);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/return")
    public ResponseEntity<?> saveReturnSaved(@Valid @RequestBody T entity) {
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        saveEntity(entity, summary);
        if (entity.getId() != null) {
            return ResponseEntity.ok(entity);
        }
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{id}")
    public ResponseEntity<T> getById(@PathVariable("id") ID id) {
        log.debug("REST request to get from {}: {}", getName(), id);
        Optional<T> activity = identifiableService.findOne(id).or(() -> identifiableService.findByUid(id.toString()));
        return ResponseUtil.wrapOrNotFound(activity);
    }

    void saveEntity(T entity, EntitySaveSummaryVM summary) {
        try {
            if (identifiableService.existsByUid(entity.getUid())) {
                entity = identifiableService.update(entity);
                summary.getUpdated().add(entity.getUid());
            } else {
                entity = identifiableService.save(entity);
                summary.getCreated().add(entity.getUid());
            }
        } catch (Exception e) {
            log.debug("REST Error Saving submission {}: {}", e.toString(), entity.getCreatedBy());
            summary.getFailed().put(entity.getUid(), e.getMessage());
        }
    }

    @DeleteMapping("/{uid}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteByIdUid(@PathVariable("uid") String uid) {
        log.debug("REST request to delete from {}: {}", getName(), uid);
        identifiableService.findByUid(uid).ifPresent((entity) -> identifiableService.deleteByUid(entity.getUid()));
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil
                .createEntityDeletionAlert(applicationName, true, getName(), uid.toString())).build();
    }


    protected Page<T> getList(Pageable pageable, QueryRequest queryRequest) {
        return identifiableService.findAllByUser(pageable, queryRequest);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<T> updateEntity(
        @PathVariable(value = "id", required = false) final ID id,
        @Valid @RequestBody T entity
    ) throws URISyntaxException {
        log.debug("REST request to delete from {}: {}", getName(), id);
        if (entity.getId() == null) {
            throw new BadRequestAlertException("Invalid id", getName(), "idnull");
        }
        if (!Objects.equals(id, entity.getId())) {
            if (!Objects.equals(id, entity.getUid())) {
                throw new BadRequestAlertException("Invalid ID", getName(), "idinvalid");
            }
        }

        if (!identifiableService.existsById(id)) {
            if (id instanceof String && !identifiableService.existsByUid(id.toString())) {
                throw new BadRequestAlertException("Entity not found", getName(), "idnotfound");
            }
        }
        entity = identifiableService.update(entity);

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, getName(), entity.getId().toString()))
            .body(entity);
    }

    protected abstract String getName();
}
