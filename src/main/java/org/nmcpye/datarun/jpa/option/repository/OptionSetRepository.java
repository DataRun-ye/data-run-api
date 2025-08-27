package org.nmcpye.datarun.jpa.option.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.option.OptionSet;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Hamza Assada
 * @since 10/09/2024
 */
@Repository
//@JaversSpringDataAuditable
public interface OptionSetRepository
    extends JpaIdentifiableRepository<OptionSet> {
    Optional<OptionSet> findByNameIgnoreCase(String name);
}
