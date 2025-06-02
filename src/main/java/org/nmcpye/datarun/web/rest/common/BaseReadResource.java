package org.nmcpye.datarun.web.rest.common;


import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.common.DRunApiVersion;
import org.nmcpye.datarun.common.AuditableObjectRepository;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.mvc.annotation.ApiVersion;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.ResponseUtil;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

//@RequestMapping("/api")
@ApiVersion({DRunApiVersion.DEFAULT, DRunApiVersion.ALL})
public abstract class BaseReadResource<T extends AuditableObject<ID>, ID extends Serializable> {

    protected final Logger log = LoggerFactory.getLogger(BaseReadResource.class);

    @Value("${jhipster.clientApp.name}")
    protected String applicationName;

    final protected AuditableObjectService<T, ID> auditableObjectService;
    final protected AuditableObjectRepository<T, ID> repository;

    protected BaseReadResource(AuditableObjectService<T, ID> auditableObjectService,
                               AuditableObjectRepository<T, ID> repository) {
        this.auditableObjectService = auditableObjectService;
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
    protected ResponseEntity<PagedResponse<?>> getAll(QueryRequest queryRequest) throws Exception {
        final var userLogin = SecurityUtils.getCurrentUserLoginOrThrow();
        log.debug("REST request to getAll {}:{}", userLogin, getName());
        Pageable pageable = queryRequest.getPageable();

        if (!queryRequest.isPaged()) {
            pageable = Pageable.unpaged();
        }

        Page<T> processedPage = getList(queryRequest, null);

        String next = createNextPageLink(processedPage);

        PagedResponse<T> response = initPageResponse(processedPage, next);
        return ResponseEntity.ok(response);
    }

    protected <E> PagedResponse<E> initPageResponse(Page<E> page, String next) {
        PagedResponse<E> response = new PagedResponse<>(page, getName(), next);
        response.setNextPage(next);
        response.setEntityName(getName());
        return response;
    }

    @PostMapping("/query")
    public ResponseEntity<PagedResponse<?>> unifiedMongoLikeQuerying(@RequestBody String jsonQuery, QueryRequest queryRequest) {
        try {
            Page<T> processedPage = getList(queryRequest, jsonQuery);

            String next = createNextPageLink(processedPage);

            PagedResponse<T> response = initPageResponse(processedPage, next);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
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

    @GetMapping("/{id}")
    public ResponseEntity<T> getById(@PathVariable("id") String id) {
        log.debug("REST request to get from {}: {}", getName(), id);
        Optional<T> entity = auditableObjectService.findByUid(id);
        return ResponseUtil.wrapOrNotFound(entity);
    }

    protected Page<T> getList(QueryRequest queryRequest, String jsonQueryBody) {
        return auditableObjectService.findAllByUser(queryRequest, jsonQueryBody);
    }

    protected abstract String getName();
}
