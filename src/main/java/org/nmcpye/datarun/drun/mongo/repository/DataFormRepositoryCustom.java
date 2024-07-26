package org.nmcpye.datarun.drun.mongo.repository;

import org.nmcpye.datarun.drun.mongo.domain.DataForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the DataForm entity.
 */
@Repository
public interface DataFormRepositoryCustom
    extends IdentifiableMongoRepository<DataForm> {

    @Query("{}")
    Page<DataForm> findAllByUser(Pageable pageable);

    @Query("{}")
    Page<DataForm> findAllWithEagerRelationshipsByUser(Pageable pageable);

    @Query("{}")
    List<DataForm> findAllWithEagerRelationshipsByUser();

    @Query("{'id': ?0}")
    Optional<DataForm> findOneWithEagerRelationshipsByUser(String id);
}
