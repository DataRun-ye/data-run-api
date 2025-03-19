package org.nmcpye.datarun.web.rest.common;

import org.nmcpye.datarun.common.IdentifiableRepository;
import org.nmcpye.datarun.common.IdentifiableService;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableEntity;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.ResponseUtil;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

//@RequestMapping("/api/custom")
public abstract class AbstractResourceRead<T extends IdentifiableEntity<ID>, ID extends Serializable> {

    protected final Logger log = LoggerFactory.getLogger(AbstractResourceRead.class);

    @Value("${jhipster.clientApp.name}")
    protected String applicationName;

    final protected IdentifiableService<T, ID> identifiableService;
    final protected IdentifiableRepository<T, ID> repository;

    protected AbstractResourceRead(IdentifiableService<T, ID> identifiableService,
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

    @GetMapping("/{id}")
    public ResponseEntity<T> getById(@PathVariable("id") String id) {
        log.debug("REST request to get from {}: {}", getName(), id);
        Optional<T> entity = identifiableService.findByUid(id);
        return ResponseUtil.wrapOrNotFound(entity);
    }

    protected Page<T> getList(Pageable pageable, QueryRequest queryRequest) {
        return identifiableService.findAllByUser(pageable, queryRequest);
    }

    protected abstract String getName();
}
