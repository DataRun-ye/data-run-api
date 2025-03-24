//package org.nmcpye.datarun.common.mongo.impl;
//
//import org.nmcpye.datarun.common.mongo.MongoBaseIdentifiableObject;
//import org.nmcpye.datarun.common.mongo.repository.MongoIdentifiableRepository;
//import org.nmcpye.datarun.common.IdentifiableService;
//import org.springframework.cache.CacheManager;
//import org.springframework.data.mongodb.core.MongoTemplate;
//
///**
// * @author Hamza, 20/03/2025
// */
//public abstract class DefaultMongoIdentifiableService<T extends MongoBaseIdentifiableObject>
//    extends DefaultMongoAuditableObjectService<T>
//    implements IdentifiableService<T, String> {
//
//    protected final MongoIdentifiableRepository<T> repository;
//
//    public DefaultMongoIdentifiableService(MongoIdentifiableRepository<T> repository,
//                                           CacheManager cacheManager, MongoTemplate mongoTemplate) {
//        super(repository, cacheManager, mongoTemplate);
//        this.repository = repository;
//    }
//}
