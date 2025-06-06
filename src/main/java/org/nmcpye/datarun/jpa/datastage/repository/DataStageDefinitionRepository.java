package org.nmcpye.datarun.jpa.datastage.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datastage.DataStageDefinition;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the DataFormTemplate entity.
 */
@Repository
@JaversSpringDataAuditable
public interface DataStageDefinitionRepository
    extends JpaIdentifiableRepository<DataStageDefinition> {
}
