package org.nmcpye.datarun.jpa.dataelement.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.dataelement.DataTemplateElement;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the DataElement entity.
 *
 * @author Hamza Assada 08/02/2024 <7amza.it@gmail.com>
 */
@SuppressWarnings("unused")
@Repository
public interface DataElementRepository
    extends JpaIdentifiableRepository<DataTemplateElement> {
    Optional<DataTemplateElement> findByNameIgnoreCase(String name);

    Optional<DataTemplateElement> findByName(String name);

    Optional<DataTemplateElement> findByCode(String code);
}
