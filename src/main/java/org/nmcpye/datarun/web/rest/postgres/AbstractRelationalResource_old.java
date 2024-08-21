//package org.nmcpye.datarun.web.rest.postgres;
//
//import jakarta.validation.Valid;
//import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
//import org.nmcpye.datarun.drun.mongo.mapping.importsummary.EntitySaveSummaryVM;
//import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
//import org.nmcpye.datarun.web.rest.common.AbstractResource;
//import org.nmcpye.datarun.web.rest.common.PagedResponse;
//import org.springdoc.core.annotations.ParameterObject;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
//
//import java.util.List;
//
//@RequestMapping("/api/custom")
//public abstract class
//AbstractRelationalResource_old<T extends IdentifiableObject<Long>>
//    extends AbstractResource<T, Long> {
//
//    protected AbstractRelationalResource_old(IdentifiableRelationalService<T> identifiableService,
//                                             JpaRepository<T, Long> repository) {
//        super(identifiableService, repository);
//    }
//
//    /**
//     * {@code GET  /Ts} : get all the entities.
//     *
//     * @param pageable  the pagination information.
//     * @param eagerload flag to eager load entities from relationships (This is for internal use only and should not be set by clients)
//     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of assignments in body.
//     */
//    @GetMapping("")
//    public ResponseEntity<PagedResponse<T>> getAllByCurrentUser(@ParameterObject Pageable pageable,
//                                                                @RequestParam(name = "paging", required = false, defaultValue = "true") boolean paging,
//                                                                @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload) {
//        if (!paging) {
//            pageable = Pageable.unpaged();
//        }
//        Page<T> page = getList(pageable, eagerload);
//        PagedResponse<T> response = new PagedResponse<>();
//        response.setPaging(paging);
//        response.setPage(page.getNumber());
//        response.setPageCount(page.getTotalPages());
//        response.setTotal(page.getTotalElements());
//        response.setPageSize(page.getSize());
//        response.setItems(page.getContent());
//        response.setNextPage(createNextPageLink(page));
//        response.setEntityName(getName());
//        return ResponseEntity.ok(response);
//    }
//
//    private String createNextPageLink(Page<?> page) {
//        if (page.hasNext()) {
//            return ServletUriComponentsBuilder.fromCurrentRequest()
//                .queryParam("page", page.getNumber() + 2) // page is 0-based, but we display it 1-based
//                .toUriString();
//        } else {
//            return null;
//        }
//    }
//
//    @PostMapping("/bulk")
//    public ResponseEntity<EntitySaveSummaryVM> saveAll(@Valid @RequestBody List<T> entities) {
//        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
//        for (T entity : entities) {
//            try {
//                if (identifiableService.existsByUid(entity.getUid())) {
//                    identifiableService.update(entity);
//                    summary.getUpdated().add(entity.getUid());
//                } else {
//                    identifiableService.save(entity);
//                    summary.getCreated().add(entity.getUid());
//                }
//            } catch (Exception e) {
//                summary.getFailed().put(entity.getUid(), e.getMessage());
//            }
//        }
//        return ResponseEntity.ok(summary);
//    }
//
//
//    @PostMapping
//    public ResponseEntity<EntitySaveSummaryVM> saveOne(@Valid @RequestBody T entity) {
//        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
//        try {
//            if (identifiableService.existsByUid(entity.getUid())) {
//                identifiableService.update(entity);
//                summary.getUpdated().add(entity.getUid());
//            } else {
//                identifiableService.save(entity);
//                summary.getCreated().add(entity.getUid());
//            }
//        } catch (Exception e) {
//            summary.getFailed().put(entity.getUid(), e.getMessage());
//        }
//        return ResponseEntity.ok(summary);
//    }
//
//
//    protected abstract Page<T> getList(Pageable pageable, boolean eagerload);
//
//    protected abstract String getName();
//}
