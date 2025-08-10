package org.nmcpye.datarun.jpa.dataelement.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the DataElement entity.
 *
 * @author Hamza Assada 08/02/2024 (7amza.it@gmail.com)
 */
@SuppressWarnings("unused")
@Repository
@JaversSpringDataAuditable
public interface DataElementRepository
    extends JpaIdentifiableRepository<DataElement> {
    Optional<DataElement> findByNameIgnoreCase(String name);

    Optional<DataElement> findByName(String name);

    Optional<DataElement> findByCode(String code);
}
