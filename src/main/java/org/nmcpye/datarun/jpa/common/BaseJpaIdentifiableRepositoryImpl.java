package org.nmcpye.datarun.jpa.common;

import io.hypersistence.utils.spring.repository.BaseJpaRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;

/**
 * @author Hamza Assada 04/06/2025 <7amza.it@gmail.com>
 */
public class BaseJpaIdentifiableRepositoryImpl<T, ID>
    extends BaseJpaRepositoryImpl<T, ID>
    implements BaseJpaIdentifiableRepository<T, ID> {

    protected final JpaEntityInformation<T, ?> entityInformation;

    protected final EntityManager entityManager;

    /**
     * Spring Data will call this constructor when instantiating each
     * repository bean (e.g. UserRepository, ProductRepository, etc.).
     *
     * @param entityInformation metadata for T→ID (so we know T.class),
     *                          injected by Spring Data JPA
     * @param entityManager     injected by Spring
     */
    public BaseJpaIdentifiableRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
    }

    @Override
    public Class<T> getEntityClass() {
        return getDomainClass();
    }

//    @Override
//    public List<T> findAll() {
//        return super.findAll();
//    }

    @Override
    public boolean canSave(T object, CurrentUserDetails user) {
        // TODO check canSave query
        return true;
    }

    @Override
    public boolean canUpdate(T object, CurrentUserDetails user) {
        // TODO check canUpdate query
        return true;
    }

    @Override
    public boolean canDelete(T object, CurrentUserDetails user) {
        // TODO check canDelete
        return true;
    }

    @Override
    public boolean canRead(T object, CurrentUserDetails user) {
        // TODO check canRead
        return true;
    }
}
