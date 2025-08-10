package org.nmcpye.datarun.jpa.option.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.option.OptionSet;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Hamza Assada 10/09/2024 (7amza.it@gmail.com)
 */
@Repository
@JaversSpringDataAuditable
public interface OptionSetRepository
    extends JpaIdentifiableRepository<OptionSet> {
    Optional<OptionSet> findByNameIgnoreCase(String name);
}
