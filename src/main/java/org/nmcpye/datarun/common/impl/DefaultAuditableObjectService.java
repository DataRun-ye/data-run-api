package org.nmcpye.datarun.common.impl;

import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.repository.AuditableObjectRepository;
import org.nmcpye.datarun.common.AuditableObjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;

import java.lang.reflect.ParameterizedType;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Hamza, 20/03/2025
 */
public abstract class DefaultAuditableObjectService<T extends AuditableObject<ID>, ID>
    implements AuditableObjectService<T, ID> {
    private static final Logger log = LoggerFactory.getLogger(DefaultAuditableObjectService.class);

    protected final AuditableObjectRepository<T, ID> repository;
    protected final CacheManager cacheManager;

    private final Class<T> klass;

    public DefaultAuditableObjectService(AuditableObjectRepository<T, ID> repository,
                                         CacheManager cacheManager) {
        this.repository = repository;
        this.cacheManager = cacheManager;
        this.klass = (Class<T>) ((ParameterizedType) getClass()
            .getGenericSuperclass())
            .getActualTypeArguments()[0];
    }

    public Class<T> getClazz() {
        return klass;
    }

    @Override
    public T saveWithRelations(T object) {
        return repository.save(object);
    }

    @Override
    public boolean existsByUid(String uid) {
        return repository.existByUid(uid);
    }

    @Override
    public Optional<T> findByUid(String uid) {
        return repository.findByUid(uid);
    }

    @Override
    public void deleteByUid(String uid) {
        repository.findByUid(uid).ifPresent(repository::delete);
    }

    @Override
    public T save(T object) {
        log.debug("Request service to save {}:`{}`", getClazz().getSimpleName(), object.getUid());
        return saveWithRelations(object);
    }

    @Override
    public T update(T object) {
        log.debug("Request service to update {}:`{}`", getClazz().getSimpleName(), object.getUid());
        T existingEntity = findByIdentifyingProperties(object)
            .orElseThrow(() ->
                new IllegalQueryException(
                    new ErrorMessage(ErrorCode.E1004,
                        getClazz().getSimpleName(), object.getUid())));
        return saveWithRelations(existingEntity);
    }

    @Override
    public Optional<T> findByIdentifyingProperties(T identifiableObject) {
        return repository
            .findByUid(identifiableObject.getUid());
    }

    @Override
    public void delete(T object) {
        findByIdentifyingProperties(object).ifPresent(repository::delete);
    }

    protected void clearCaches(String name, String key) {
        if (name != null && key != null) {
            Objects.requireNonNull(cacheManager.getCache(name)).evict(key);
        }
    }
}
