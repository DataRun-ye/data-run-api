package org.nmcpye.datarun.jpa.reference;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferenceEntryRepository
    extends BaseJpaIdentifiableRepository<ReferenceEntry, String> {

    Page<ReferenceEntry> findAllByOrgUnitIdOrderByUidAsc(
        String orgUnitId,
        Pageable pageable);
}
