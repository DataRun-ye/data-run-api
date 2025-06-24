package org.nmcpye.datarun.web.rest.common;


import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.acl.AclService;
import org.nmcpye.datarun.common.DRunApiVersion;
import org.nmcpye.datarun.common.IdentifiableObject;
import org.nmcpye.datarun.common.IdentifiableObjectRepository;
import org.nmcpye.datarun.common.IdentifiableObjectService;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.mvc.annotation.ApiVersion;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.ResponseUtil;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

//@RequestMapping("/api")
@ApiVersion({DRunApiVersion.DEFAULT, DRunApiVersion.ALL})
public abstract class BaseReadResource<T extends IdentifiableObject<ID>, ID extends Serializable> {

    protected final Logger log = LoggerFactory.getLogger(BaseReadResource.class);

    @Value("${jhipster.clientApp.name}")
    protected String applicationName;

    final protected IdentifiableObjectService<T, ID> identifiableObjectService;
    final protected IdentifiableObjectRepository<T, ID> repository;

    @Autowired
    protected AclService aclService;

    protected BaseReadResource(IdentifiableObjectService<T, ID> identifiableObjectService,
                               IdentifiableObjectRepository<T, ID> repository) {
        this.identifiableObjectService = identifiableObjectService;
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
    protected ResponseEntity<PagedResponse<?>> getAll(QueryRequest queryRequest) {
        Page<T> processedPage = getList(queryRequest, null);

        String next = createNextPageLink(processedPage);

        PagedResponse<T> response = initPageResponse(processedPage, next);
        return ResponseEntity.ok(response);
    }

    /**
     * minimal Access rights or throw
     *
     * @param currentUser user
     * @throws ResponseStatusException exception if has no business here whatsoever (no minimal rights)
     */
    protected void hasMinimalRightsOrThrow(CurrentUserDetails currentUser) throws ResponseStatusException {
        if (currentUser == null || !aclService.hasMinimalRights(currentUser)) {
            log.warn("REST Prevent Access, no minimal rights `{}`:`{}`", getEntityClass().getSimpleName(), currentUser);
            throw new IllegalQueryException(HttpStatus.FORBIDDEN + ", You Hava No Business Here");
        }
    }

    protected <E> PagedResponse<E> initPageResponse(Page<E> page, String next) {
        PagedResponse<E> response = new PagedResponse<>(page, getName(), next);
        response.setNextPage(next);
        response.setEntityName(getName());
        return response;
    }

    @PostMapping("/query")
    public ResponseEntity<PagedResponse<?>> unifiedMongoLikeQuerying(QueryRequest queryRequest,
                                                                     @RequestBody(required = false) String jsonQuery) {
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
    public ResponseEntity<T> getById(@PathVariable("id") ID id,
                                     @AuthenticationPrincipal CurrentUserDetails user) {
        hasMinimalRightsOrThrow(user);
        log.debug("REST request to get from {}: {}", getName(), id);
        Optional<T> entity = identifiableObjectService.findById(id);
        return ResponseUtil.wrapOrNotFound(entity);
    }

    protected Page<T> getList(QueryRequest queryRequest, String jsonQueryBody) {
        final var user = SecurityUtils.getCurrentUserDetailsOrThrow();
        log.debug("REST request to getList {}:{}", user.getUsername(), getName());
        if (!aclService.hasMinimalRights(user)) {
            log.warn("REST Prevent Access to `{}`, no minimal rights: `{}`", getEntityClass().getSimpleName(), user);
            return Page.empty();
        }
        return identifiableObjectService.findAllByUser(queryRequest, jsonQueryBody)
            .map(s -> postProcess(s, queryRequest, jsonQueryBody));
    }

    protected T postProcess(T entity, QueryRequest queryRequest, String jsonQuery) {
        return entity;
    }

    private T findByIdOrUid(T entity) {
        return Optional.ofNullable(entity.getId())
            .flatMap(identifiableObjectService::findById)
            .or(() -> Optional.ofNullable(entity.getUid())
                .flatMap(identifiableObjectService::findByUid))
            .orElseThrow(() -> new PropertyNotFoundException("OrgUnit not found: " + entity));
    }

    protected abstract String getName();
}
