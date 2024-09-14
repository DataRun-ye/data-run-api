package org.nmcpye.datarun.drun.mongo.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.drun.mongo.repository.IdentifiableMongoRepository;
import org.nmcpye.datarun.drun.mongo.service.IdentifiableMongoService;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public abstract class IdentifiableMongoServiceImpl<T extends IdentifiableObject<String>>
    implements IdentifiableMongoService<T> {
    private final Logger log = LoggerFactory.getLogger(IdentifiableMongoServiceImpl.class);

    protected final IdentifiableMongoRepository<T> repository;

    public IdentifiableMongoServiceImpl(IdentifiableMongoRepository<T> repository) {
        this.repository = repository;
    }


    @Override
    public boolean existsByUid(String uid) {
        return repository.findByUid(uid).isPresent();
    }

    @Override
    public boolean existsById(String id) {
        return repository.findById(id).isPresent();
    }

    @Override
    public Optional<T> findByUid(String uid) {
        return repository.findByUid(uid);
    }

    @Override
    public Optional<T> findByCode(String code) {
        return repository.findByCode(code);
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

    @Override
    public T saveWithRelations(T object) {
        return repository.save(object);
    }

    @Override
    public T update(T object) {
        T existingEntity = repository
            .findByUid(object.getUid())
//            .or(() -> repository
//                .findById(object.getId()))
            .orElseThrow(() -> new PropertyNotFoundException("Entity not found with UID: " + object.getUid()));

        log.debug("Request to update T : {}", object);
        object.setId(existingEntity.getId());
        return saveWithRelations(object);
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        log.debug("Request to get all MEntities");
        return repository.findAll(pageable);
    }

    @Override
    public Page<T> findAllByUser(Pageable pageable) {
        log.debug("Request to get all MEntities");
        return this.findAll(pageable);
    }

    @Override
    public Page<T> findAllWithEagerRelationships(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Optional<T> findOne(String id) {
        log.debug("Request to get T : {}", id);
        return repository.findById(id);
    }

    @Override
    public void delete(String id) {
        log.debug("Request to delete T : {}", id);
        repository.deleteById(id);
    }
}
