package org.nmcpye.datarun.web.rest.common;

import jakarta.validation.Valid;
import org.nmcpye.datarun.domain.common.IdentifiableObject;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.nmcpye.datarun.service.dto.drun.SaveSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RequestMapping("/api/custom")
public abstract class AbstractResource<T extends IdentifiableObject<Long>> {

    private final Logger log = LoggerFactory.getLogger(AbstractResource.class);

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    final protected IdentifiableRelationalService<T> identifiableService;
    final protected JpaRepository<T, Long> repository;

    protected AbstractResource(IdentifiableRelationalService<T> identifiableService,
                               JpaRepository<T, Long> repository) {
        this.identifiableService = identifiableService;
        this.repository = repository;
    }

    /**
     * {@code GET  /Ts} : get all the entities.
     *
     * @param pageable  the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is for internal use only and should not be set by clients)
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of assignments in body.
     */
    @GetMapping("")
    public ResponseEntity<PagedResponse<T>> getAllByCurrentUser(@ParameterObject Pageable pageable,
                                                                @RequestParam(name = "paging", required = false, defaultValue = "true") boolean paging,
                                                                @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload) {
        if (!paging) {
            pageable = Pageable.unpaged();
        }
        Page<T> page = getList(pageable, eagerload);
        PagedResponse<T> response = new PagedResponse<>();
        response.setPaging(paging);
        response.setPage(page.getNumber());
        response.setPageCount(page.getTotalPages());
        response.setTotal(page.getTotalElements());
        response.setPageSize(page.getSize());
        response.setItems(page.getContent());
        response.setNextPage(createNextPageLink(page));
        response.setEntityName(getName());
        return ResponseEntity.ok(response);
    }

    private String createNextPageLink(Page<?> page) {
        if (page.hasNext()) {
            return ServletUriComponentsBuilder.fromCurrentRequest()
                .queryParam("page", page.getNumber() + 2) // page is 0-based, but we display it 1-based
                .toUriString();
        } else {
            return null;
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<SaveSummary> saveAll(@Valid @RequestBody List<T> entities) {
        SaveSummary summary = new SaveSummary();
        for (T entity : entities) {
            try {
                if (identifiableService.existsByUid(entity.getUid())) {
                    identifiableService.update(entity);
                    summary.getUpdated().add(entity.getUid());
                } else {
                    identifiableService.save(entity);
                    summary.getCreated().add(entity.getUid());
                }
            } catch (Exception e) {
                summary.getFailed().put(entity.getUid(), e.getMessage());
            }
        }
        return ResponseEntity.ok(summary);
    }


    @PostMapping
    public ResponseEntity<SaveSummary> saveOne(@Valid @RequestBody T entity) {
        SaveSummary summary = new SaveSummary();
        try {
            if (identifiableService.existsByUid(entity.getUid())) {
                identifiableService.update(entity);
                summary.getUpdated().add(entity.getUid());
            } else {
                identifiableService.save(entity);
                summary.getCreated().add(entity.getUid());
            }
        } catch (Exception e) {
            summary.getFailed().put(entity.getUid(), e.getMessage());
        }
        return ResponseEntity.ok(summary);
    }


    protected abstract Page<T> getList(Pageable pageable, boolean eagerload);

    protected abstract String getName();
}
