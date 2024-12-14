package org.nmcpye.datarun.drun.postgres.service.indentifieble;

import jakarta.persistence.EntityNotFoundException;
import org.nmcpye.datarun.drun.postgres.common.Identifiable;
import org.nmcpye.datarun.drun.postgres.repository.IdentifiableRelationalRepository;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public abstract class IdentifiableRelationalServiceImpl<T extends Identifiable<Long>>
    implements IdentifiableRelationalService<T> {

    private static final Logger log = LoggerFactory.getLogger(IdentifiableRelationalServiceImpl.class);

    protected final IdentifiableRelationalRepository<T> repository;

    public IdentifiableRelationalServiceImpl(IdentifiableRelationalRepository<T> repository) {
        this.repository = repository;
    }

    public Specification<T> hasAccess() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    @Override
    public Page<T> findAll(Specification<T> spec, Pageable pageable) {
        return repository.findAll(spec.and(hasAccess()), pageable);
    }

    @Override
    public <Q extends QueryRequest> List<T> findAll(Q queryRequest) {
        return repository.findAll();
    }

    @Override
    public List<T> findAll(Specification<T> spec) {
        return repository.findAll(spec.and(hasAccess()));
    }

    @Override
    public Optional<T> findOne(Specification<T> spec) {
        return repository.findOne(spec.and(hasAccess()));
    }

    @Override
    public Optional<T> findIdentifiable(Identifiable<Long> identifiableObject) {
        return repository.findByUid(identifiableObject.getUid()).or(() -> repository.findById(identifiableObject.getId()));

    }

    @Override
    public boolean existsByUid(String uid) {
        return repository.findByUid(uid).isPresent();
    }

    @Override
    public boolean existsById(Long id) {
        return repository.findById(id).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findByUid(String uid) {
        return repository.findByUid(uid);
    }

    @Override
    public void deleteByUid(String uid) {
        repository.findByUid(uid).ifPresent(repository::delete);
    }

    @Override
    public <Q extends QueryRequest> Page<T> findAllByUser(Pageable pageable, Q queryRequest) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(pageable);
        }
        log.debug("Request to get all entities");
        return repository.findAll(hasAccess(), pageable);
    }

    @Override
    public <Q extends QueryRequest> List<T> findAllByUser(Q queryRequest) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll();
        }
        log.debug("Request to get all entities");
        return repository.findAll(hasAccess());
    }

    @Override
    public T save(T object) {
        log.debug("Request to save Identifiable T : {}", object.getUid());
        if (object.getUid() == null || object.getUid().isEmpty()) {
            object.setUid(CodeGenerator.generateUid());
        }
        return saveWithRelations(object);
    }

    @Override
    public T saveWithRelations(T object) {
        return repository.save(object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T update(T object) {
        log.debug("Request to update T : {}", object);
        T existingEntity = findIdentifiable(object)
            .orElseThrow(() -> new EntityNotFoundException("Entity not found with UID: " + object.getUid()));

        object.setId(existingEntity.getId());

        object.setIsPersisted();
        return saveWithRelations(object);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable pageable, QueryRequest queryRequest) {
        log.debug("Request to get all entities");
        return repository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findOne(Long id) {
        log.debug("Request to get T : {}", id);
        return repository.findById(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete T : {}", id);
        repository.findById(id).ifPresent(repository::delete);
    }
}
