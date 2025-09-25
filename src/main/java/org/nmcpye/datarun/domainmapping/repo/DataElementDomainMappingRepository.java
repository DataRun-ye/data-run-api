package org.nmcpye.datarun.domainmapping.repo;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.nmcpye.datarun.domainmapping.model.DataElementDomainMapping;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Hamza Assada
 * @since 23/09/2025
 */
@Repository
public interface DataElementDomainMappingRepository extends BaseJpaRepository<DataElementDomainMapping, String> {
    List<DataElementDomainMapping> findPublishedByTemplateVersion(String templateVersionUid);
}
