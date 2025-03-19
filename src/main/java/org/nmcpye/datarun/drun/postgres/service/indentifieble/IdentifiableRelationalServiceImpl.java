package org.nmcpye.datarun.drun.postgres.service.indentifieble;

import jakarta.persistence.EntityNotFoundException;
import org.nmcpye.datarun.drun.postgres.common.IdentifiableEntity;
import org.nmcpye.datarun.drun.postgres.repository.IdentifiableRelationalRepository;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Transactional
public abstract class IdentifiableRelationalServiceImpl
    <T extends IdentifiableEntity<Long>>
    implements IdentifiableRelationalService<T> {

    private static final Logger log = LoggerFactory.getLogger(IdentifiableRelationalServiceImpl.class);

    protected final IdentifiableRelationalRepository<T> identifiableRepository;

    private final CacheManager cacheManager;

    public IdentifiableRelationalServiceImpl(IdentifiableRelationalRepository<T> identifiableRepository, CacheManager cacheManager) {
        this.identifiableRepository = identifiableRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    public Optional<T> findIdentifiable(IdentifiableEntity<Long> identifiableObject) {
        return identifiableRepository.findByUid(identifiableObject.getUid()).or(() -> identifiableRepository.findById(identifiableObject.getId()));

    }

    @Override
    public boolean existsByUid(String uid) {
        return identifiableRepository.findByUid(uid).isPresent();
    }

    @Override
    public boolean existsById(Long id) {
        return identifiableRepository.findById(id).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findByUid(String uid) {
        return identifiableRepository.findByUid(uid);
    }

    @Override
    public void deleteByUid(String uid) {
        identifiableRepository.findByUid(uid).ifPresent(identifiableRepository::delete);
    }

    @Override
    public Page<T> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return identifiableRepository.findAll(pageable);
        }
        log.debug("Request to get all entities");
        return identifiableRepository.findAll(canRead(), pageable);
    }

    @Override
    public Page<T> findAllByUser(Specification<T> spec, Pageable pageable) {
        log.debug("Request to get all entities");
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return identifiableRepository.findAll(spec, pageable);
        }

        return identifiableRepository.findAll(appendReadSpecification(spec), pageable);
    }

    @Override
    public List<T> findAllByUser(QueryRequest queryRequest) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return identifiableRepository.findAll();
        }
        log.debug("Request to get all entities");
        return identifiableRepository.findAll(canRead());
    }

    @Override
    public T save(T object) {
        log.debug("Request to save Identifiable T : {}", object.getUid());
        return saveWithRelations(object);
    }

    @Override
    public T saveWithRelations(T object) {
        return identifiableRepository.save(object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T update(T object) {
        log.debug("Request to update T : {}", object);
        T existingEntity = findIdentifiable(object)
            .orElseThrow(() -> new EntityNotFoundException("Entity not found with UID: " + object.getUid()));

        object.setId(existingEntity.getId());
        object.setCreatedBy(existingEntity.getCreatedBy());

        object.setIsPersisted();
        return saveWithRelations(object);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete T : {}", id);
        identifiableRepository.findById(id).ifPresent(identifiableRepository::delete);
    }

    protected void clearCaches(String name, String key) {
        if (name != null && key != null) {
            Objects.requireNonNull(cacheManager.getCache(name)).evict(key);
        }
    }
}
