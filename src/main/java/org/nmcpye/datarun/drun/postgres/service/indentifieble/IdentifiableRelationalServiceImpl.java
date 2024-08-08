package org.nmcpye.datarun.drun.postgres.service.indentifieble;

import jakarta.persistence.EntityNotFoundException;
import org.nmcpye.datarun.domain.common.IdentifiableObject;
import org.nmcpye.datarun.drun.postgres.repository.IdentifiableRelationalRepository;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
public abstract class IdentifiableRelationalServiceImpl<T extends IdentifiableObject<Long>>
    implements IdentifiableRelationalService<T> {

    private static final Logger log = LoggerFactory.getLogger(IdentifiableRelationalServiceImpl.class);

    protected final IdentifiableRelationalRepository<T> repository;

    public IdentifiableRelationalServiceImpl(IdentifiableRelationalRepository<T> repository) {
        this.repository = repository;
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
        return repository.findAll(pageable);
    }

    public Page<T> findAllWithEagerRelationships(Pageable pageable) {
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
