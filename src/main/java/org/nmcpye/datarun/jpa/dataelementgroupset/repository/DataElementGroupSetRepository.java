package org.nmcpye.datarun.jpa.dataelementgroupset.repository;

import org.nmcpye.datarun.jpa.common.repository.JpaAuditableRepository;
import org.nmcpye.datarun.jpa.dataelementgroupset.DataElementGroupSet;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DataElementGroupSetRepository
    extends JpaAuditableRepository<DataElementGroupSet> {

    Optional<DataElementGroupSet> findByCode(String code);
}
