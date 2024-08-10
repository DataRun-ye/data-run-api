package org.nmcpye.datarun.drun.mongo.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.drun.mongo.domain.AssignmentMongo;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the Assignment entity.
 */
@Repository
@JaversSpringDataAuditable
public interface AssignmentRepositoryCustom
    extends IdentifiableMongoRepository<AssignmentMongo> {
}
