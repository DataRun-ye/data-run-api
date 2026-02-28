package org.nmcpye.datarun.jpa.datasubmission.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public interface DataSubmissionRepository
    extends JpaIdentifiableRepository<DataSubmission> {
    @Override
    default Optional<DataSubmission> findFirstByCode(String code) {
        return Optional.empty();
    }

    @Override
    default List<DataSubmission> findAllByCodeIn(Collection<String> codes) {
        return Collections.emptyList();
    }

    @Override
    default Boolean existsByCode(String code) {
        return false;
    }

    @Override
    default Optional<DataSubmission> findFirstByName(String name) {
        return Optional.empty();
    }

    default List<DataSubmission> findByNameLike(String name) {
        return Collections.emptyList();
    }

    Page<DataSubmission> findBySerialNumberGreaterThanAndFormInOrderBySerialNumberAsc(Long lastSerial, List<String> uids, Pageable pageable);

    List<DataSubmission> findBySerialNumberBetween(Long serialNumberAfter, Long serialNumberBefore);

    Integer countBySerialNumberGreaterThan(Long serialNumberIsGreaterThan);
}
