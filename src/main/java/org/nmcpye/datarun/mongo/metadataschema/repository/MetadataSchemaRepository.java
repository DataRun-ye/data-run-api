package org.nmcpye.datarun.mongo.metadataschema.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.mongo.common.repository.MongoIdentifiableRepository;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.domain.MetadataSchema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the DataFormSubmission entity.
 */
@Repository
@JaversSpringDataAuditable
public interface MetadataSchemaRepository extends MongoIdentifiableRepository<MetadataSchema> {
    @Query("{ 'team.userInfo.login' : ?#{authentication.name} }")
    Page<DataFormSubmission> findAllByUser(Pageable pageable);
}
