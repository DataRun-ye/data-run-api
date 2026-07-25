package org.nmcpye.datarun.jpa.reference;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferenceEntryRepository
    extends BaseJpaIdentifiableRepository<ReferenceEntry, String> {

    Optional<ReferenceEntry> findByUid(String uid);

    List<ReferenceEntry> findAllByUidIn(Collection<String> uids);

    Page<ReferenceEntry> findAllByOrgUnitIdOrderByUidAsc(
        String orgUnitId,
        Pageable pageable);
}
