package org.nmcpye.datarun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.common.mongo.repository.MongoAuditableRepository;
import org.nmcpye.datarun.mongo.domain.DataForm;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the DataForm entity.
 */
@Repository
@JaversSpringDataAuditable
public interface DataFormRepository extends MongoAuditableRepository<DataForm> {

    @Query("{'activity': ?0}")
    List<DataForm> findAllByActivity(String activity);

    //    @Query(value = "{ 'form': { $in: ?0 }, 'permissions': ?1 }", fields = "{ 'form': 1, '_id': 0 }")
//    List<String> findByFormsAndPermission(List<String> form, String permission);
//    @Query(value = "{ 'uid': { $in: ?0 }}")
//    Page<DataForm> findAllByUidIn(List<String> uids, Pageable pageable);
//
    List<DataForm> findAllByUidIn(List<String> uids);
}
