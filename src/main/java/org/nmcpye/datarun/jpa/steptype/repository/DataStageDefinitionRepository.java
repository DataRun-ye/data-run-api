package org.nmcpye.datarun.jpa.steptype.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.steptype.StepType;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the DataFormTemplate entity.
 */
@Repository
@JaversSpringDataAuditable
public interface DataStageDefinitionRepository
    extends JpaIdentifiableRepository<StepType> {
}
