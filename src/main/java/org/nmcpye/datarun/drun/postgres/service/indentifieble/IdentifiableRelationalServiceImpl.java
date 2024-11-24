package org.nmcpye.datarun.drun.postgres.service.indentifieble;

import jakarta.persistence.EntityNotFoundException;
import org.nmcpye.datarun.drun.common.IdentifiableSpecifications;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.nmcpye.datarun.drun.postgres.repository.IdentifiableRelationalRepository;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public abstract class IdentifiableRelationalServiceImpl<T extends IdentifiableObject<Long>>
    extends IdentifiableSpecifications<T>
    implements IdentifiableRelationalService<T> {

    private static final Logger log = LoggerFactory.getLogger(IdentifiableRelationalServiceImpl.class);

    protected final IdentifiableRelationalRepository<T> repository;

    public IdentifiableRelationalServiceImpl(IdentifiableRelationalRepository<T> repository) {
        this.repository = repository;
    }

    @Override
    public Page<T> findAll(Specification<T> spec, Pageable pageable) {
        return repository.findAll(spec, pageable);
    }

    @Override
    public List<T> findAll(Specification<T> spec) {
        return repository.findAll(spec);
    }

    @Override
    public Optional<T> findOne(Specification<T> spec) {
        return repository.findOne(spec);
    }

    @Override
    public Optional<T> findIdentifiable(IdentifiableObject<Long> identifiableObject) {
        var spec = hasUid(identifiableObject.getUid())
            .or(hasId(identifiableObject.getId()));
        return repository.findOne(spec);
    }

    @Override
    public boolean existsByUid(String uid) {
        return repository.exists(hasUid(uid));
    }

    @Override
    public boolean existsById(Long id) {
        return repository.exists(hasId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findByUid(String uid) {
        return repository.findOne(hasUid(uid));
    }

    @Override
    public Optional<T> findOneByCode(String code) {
        return repository.findOne(hasCode(code));
    }

    @Override
    public void deleteByUid(String uid) {
        repository.findOne(hasUid(uid)).ifPresent(repository::delete);
    }

    @Override
    public T save(T object) {
        log.debug("Request to save Identifiable T : {}", object.getName());
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
    public Page<T> findAll(Pageable pageable) {
        log.debug("Request to get all entities");
        return repository.findAll(pageable);
    }

    public Page<T> findAllWithEagerRelationships(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Page<T> findAllByUser(Pageable pageable) {
        log.debug("Request to get all entities");
        return this.findAll(pageable);
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
