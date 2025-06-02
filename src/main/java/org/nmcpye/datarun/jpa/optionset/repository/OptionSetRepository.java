package org.nmcpye.datarun.jpa.optionset.repository;

import org.nmcpye.datarun.jpa.common.repository.JpaAuditableRepository;
import org.nmcpye.datarun.jpa.optionset.OptionSet;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OptionSetRepository
    extends JpaAuditableRepository<OptionSet> {
    Optional<OptionSet> findByNameIgnoreCase(String name);
}
