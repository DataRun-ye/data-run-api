package org.nmcpye.datarun.jpa.dataelementgroup.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.dataelementgroup.DataElementGroup;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DataElementGroupRepository
    extends JpaIdentifiableRepository<DataElementGroup> {

    Optional<DataElementGroup> findByCode(String code);
}
