//package org.nmcpye.datarun.common.jpa.impl;
//
//import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;
//import org.nmcpye.datarun.common.jpa.repository.JpaIdentifiableRepository;
//import org.nmcpye.datarun.common.IdentifiableService;
//import org.nmcpye.datarun.useraccess.UserAccessService;
//import org.springframework.cache.CacheManager;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Optional;
//
//import static org.nmcpye.datarun.drun.postgres.common.IdentifiableAccessSpecification.hasCode;
//
///**
// * @author Hamza, 20/03/2025
// */
//@Transactional
//public abstract class DefaultJpaIdentifiableService
//    <T extends JpaBaseIdentifiableObject> extends DefaultJpaAuditableService<T>
//    implements IdentifiableService<T, Long> {
//
//    protected final JpaIdentifiableRepository<T> jpaIdentifiableRepository;
//
//    public DefaultJpaIdentifiableService(JpaIdentifiableRepository<T> jpaIdentifiableRepository,
//                                         CacheManager cacheManager, UserAccessService userAccessService) {
//        super(jpaIdentifiableRepository, cacheManager, userAccessService);
//        this.jpaIdentifiableRepository = jpaIdentifiableRepository;
//    }
//
//    @Override
//    public Optional<T> findByIdentifyingProperties(T identifiableObject) {
//        return jpaIdentifiableRepository
//            .findByUid(identifiableObject.getUid())
//            .or(() ->
//                jpaIdentifiableRepository.findOne(hasCode(identifiableObject.getCode())));
//    }
//}
