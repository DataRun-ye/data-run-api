package org.nmcpye.datarun.jpa.dataelement.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the DataElement entity.
 *
 * @author Hamza Assada
 * @since 08/02/2024
 */
@SuppressWarnings("unused")
@Repository
//@JaversSpringDataAuditable
public interface DataElementRepository
    extends JpaIdentifiableRepository<DataElement> {
    Optional<DataElement> findByNameIgnoreCase(String name);

    Optional<DataElement> findByName(String name);

    Optional<DataElement> findByCode(String code);
}
