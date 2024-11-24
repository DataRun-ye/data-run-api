package org.nmcpye.datarun.drun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.drun.mongo.domain.DataForm;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for the DataForm entity.
 */
@Repository
@JaversSpringDataAuditable
public interface DataFormRepository
    extends IdentifiableMongoRepository<DataForm> {

    @Query("{'activity': ?0}")
    List<DataForm> findAllByActivity(String activity);
}
