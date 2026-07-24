package org.nmcpye.datarun.jpa.reference;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferenceEntryRepository extends JpaIdentifiableRepository<ReferenceEntry> {

    Page<ReferenceEntry> findAllByOrgUnitIdOrderByUidAsc(String orgUnitId, Pageable pageable);
}
