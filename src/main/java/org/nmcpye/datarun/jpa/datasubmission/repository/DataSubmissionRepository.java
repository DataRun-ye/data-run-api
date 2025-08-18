package org.nmcpye.datarun.jpa.datasubmission.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the DataFormTemplate entity.
 */
@Repository
public interface DataSubmissionRepository
    extends JpaIdentifiableRepository<DataSubmission> {
    List<DataSubmission> findBySerialNumberIsNull();

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

}
