//package org.nmcpye.datarun.mongo.legacydatatemplate.repository;
//
//import org.javers.spring.annotation.JaversSpringDataAuditable;
//import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
//import org.nmcpye.datarun.mongo.common.repository.MongoIdentifiableRepository;
//import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
//import org.springframework.data.mongodb.repository.Query;
//import org.springframework.stereotype.Repository;
//
//import java.util.Collection;
//import java.util.List;
//import java.util.Set;
//
///**
// * Spring Data MongoDB repository for the DataFormTemplate entity.
// */
//@Repository
//@JaversSpringDataAuditable
//public interface DataFormTemplateRepository
//    extends MongoIdentifiableRepository<DataFormTemplate> {
//    Set<DataFormTemplate> findAllByUidInAndDisabledIsNot(Collection<String> uids, Boolean disabled);
//
//    @Query(value = "{ 'fields.type': { $in: ?0 }}")
//    List<DataFormTemplate> findByFieldType(List<ValueType> types);
//}
