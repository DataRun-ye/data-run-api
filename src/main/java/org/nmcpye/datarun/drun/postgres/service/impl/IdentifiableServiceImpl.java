package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.nmcpye.datarun.domain.common.IdentifiableObject;
import org.nmcpye.datarun.drun.postgres.repository.IdentifiableRepository;
import org.nmcpye.datarun.drun.postgres.service.IdentifiableService;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public abstract class IdentifiableServiceImpl<T extends IdentifiableObject<Long>> implements IdentifiableService<T> {

    private final Logger log = LoggerFactory.getLogger(IdentifiableServiceImpl.class);

    protected final IdentifiableRepository<T> repository;

    public IdentifiableServiceImpl(IdentifiableRepository<T> repository) {
        this.repository = repository;
    }

    @Override
    public T saveWithRelations(T object) {
        log.debug("Request to save T With Relations, saveWithRelations not implement will perform a normal save: {}", object);
        return repository.save(object);
    }

    @Override
    public boolean existsByUid(String uid) {
        return repository.findByUid(uid).isPresent();
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
    public T save(T object) {
        if (object.getUid() == null || object.getUid().isEmpty()) {
            object.setUid(CodeGenerator.generateUid());
        }
        return saveWithRelations(object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T update(T object) {
        T existingEntity = repository
            .findByUid(object.getUid())
            .orElseThrow(() -> new EntityNotFoundException("Entity not found with UID: " + object.getUid()));

        log.debug("Request to update T : {}", object);
        //            T updatedEntity = existingEntity.get();
        object.setId(existingEntity.getId());
        // Update fields
        object.setIsPersisted();
        return saveWithRelations(object);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable pageable) {
        log.debug("Request to get all ChvRegisters");
        return repository.findAllByUser(pageable);
    }

    public Page<T> findAllWithEagerRelationships(Pageable pageable) {
        return repository.findAllWithEagerRelationshipsByUser(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findOne(Long id) {
        log.debug("Request to get T : {}", id);
        return repository.findOneWithEagerRelationshipsByUser(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete T : {}", id);
        repository.deleteById(id);
    }
}
