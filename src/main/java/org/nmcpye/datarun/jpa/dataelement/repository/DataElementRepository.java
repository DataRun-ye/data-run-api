package org.nmcpye.datarun.jpa.dataelement.repository;

import org.nmcpye.datarun.jpa.common.repository.JpaAuditableRepository;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DataElementRepository
    extends JpaAuditableRepository<DataElement> {
    Optional<DataElement> findByNameIgnoreCase(String name);
    Optional<DataElement> findByName(String name);
    Optional<DataElement> findByCode(String code);
}
