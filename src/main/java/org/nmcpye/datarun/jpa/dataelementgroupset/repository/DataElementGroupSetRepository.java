package org.nmcpye.datarun.jpa.dataelementgroupset.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.dataelementgroupset.DataElementGroupSet;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DataElementGroupSetRepository
    extends JpaIdentifiableRepository<DataElementGroupSet> {

    Optional<DataElementGroupSet> findByCode(String code);
}
