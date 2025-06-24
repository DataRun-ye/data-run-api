package org.nmcpye.datarun.common;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.ParameterizedType;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Hamza Assada 20/03/2025 <7amza.it@gmail.com>
 */
@Transactional
public abstract class DefaultIdentifiableObjectService<T extends IdentifiableObject<ID>, ID>
    implements IdentifiableObjectService<T, ID> {
    private static final Logger log = LoggerFactory.getLogger(DefaultIdentifiableObjectService.class);

    protected final IdentifiableObjectRepository<T, ID> repository;
    protected final CacheManager cacheManager;

    protected final Class<T> klass;

    @SuppressWarnings("unchecked")
    public DefaultIdentifiableObjectService(IdentifiableObjectRepository<T, ID> repository,
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
        return save(object);
    }

    @Override
    public boolean existsByUid(String uid) {
        return repository.existsByUid(uid);
    }


    @Override
    public Optional<T> findByUid(String uid) {
        return repository.findByUid(uid);
    }

    @Override
    public void deleteByUid(String uid) {
        findByUid(uid).ifPresent(repository::delete);
    }

    @Override
    public Optional<T> findById(ID id) {
        return repository.findById(id);
    }

    @Override
    public boolean existsById(ID uid) {
        return repository.existsById(uid);
    }

    @Override
    public void deleteById(ID uid) {
        findById(uid).ifPresent(repository::delete);
    }

    @Override
    public T save(T object) {
        log.debug("Request service to save {}:`{}`", getClazz().getSimpleName(), object.getId());
        return repository.save(object);
    }

    @Override
    @Transactional
    public T update(T object) {
        log.debug("Request service to update {}:`{}`", getClazz().getSimpleName(), object.getId());
        T existingEntity = findByIdOrUid(object)
            .orElseThrow(() ->
                new IllegalQueryException(
                    new ErrorMessage(ErrorCode.E1004,
                        getClazz().getSimpleName(), object.getId())));

        object.setId(existingEntity.getId());
        object.setCreatedBy(existingEntity.getCreatedBy());
        /// update object, overwrite with updates
        return saveWithRelations(object);
    }

    @Override
    public void delete(T object) {
        deleteById(object.getId());
    }

    protected void clearCaches(String name, String key) {
        if (name != null && key != null) {
            Objects.requireNonNull(cacheManager.getCache(name)).evict(key);
        }
    }

    @Override
    public Optional<T> findByIdOrUid(T entity) {
        return Optional.ofNullable(entity.getUid())
            .flatMap(repository::findByUid)
            .or(() -> Optional.ofNullable(entity.getId())
                .flatMap(repository::findById));
    }
}
