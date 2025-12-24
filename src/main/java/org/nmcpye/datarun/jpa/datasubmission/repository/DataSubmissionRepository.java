package org.nmcpye.datarun.jpa.datasubmission.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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


    @Query(value = "SELECT form_data FROM data_submission WHERE uid = :uid", nativeQuery = true)
    String findFormDataJsonByUid(@Param("uid") String uid);

    @Query(value = "SELECT uid FROM data_submission " +
        "WHERE (form_data #> string_to_array(:path, '.')) IS NOT NULL " +
        "ORDER BY uid LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<String> findSubmissionUidsWithPath(@Param("path") String path, @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = "SELECT template_version_uid FROM data_submission " +
        "WHERE uid = :uid", nativeQuery = true)
    String findTemplateVersionUidByUid(@Param("uid") String uid);

    // page by serialNumber cursor
    Page<DataSubmission> findBySerialNumberGreaterThanOrderBySerialNumberAsc(Long lastSerial, Pageable pageable);

    Page<DataSubmission> findBySerialNumberGreaterThanAndFormInOrderBySerialNumberAsc(Long lastSerial, List<String> uids, Pageable pageable);

    List<DataSubmission> findBySerialNumberBetween(Long serialNumberAfter, Long serialNumberBefore);

    Integer countBySerialNumberGreaterThan(Long serialNumberIsGreaterThan);
}
