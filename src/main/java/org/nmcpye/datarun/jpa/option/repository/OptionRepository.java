package org.nmcpye.datarun.jpa.option.repository;

import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.option.Option;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Hamza Assada
 * @since 30/06/2025
 */
@SuppressWarnings("unused")
@Repository
public interface OptionRepository
    extends JpaIdentifiableRepository<Option>, OptionRepositoryCustom {
    String OPTIONS_OPTION_SET_UID = "userTeamIdsByLogin";

    // no-op
    @Override
    default void deleteByUid(String uid) {
    }

    @Override
    default void deleteAllByUidIn(Collection<String> uids) {
    }

    @Cacheable(cacheNames = OPTIONS_OPTION_SET_UID)
    List<Option> findAllByOptionSetUid(@Size(max = 11) String optionSetUid);

    // Native query that looks inside the translations JSONB array for exact match
    // find by code scoped to option-set uid (join through option_set)
    @Query(value = """
        SELECT ov.* FROM option_value ov
        JOIN option_set os ON ov.option_set_id = os.id
        WHERE os.uid = :setUid
          AND ov.code = :code
        LIMIT 1
        """, nativeQuery = true)
    Optional<Option> findByCodeAndOptionSetUid(@Param("code") String code, @Param("setUid") String optionSetUid);

    // find by name scoped to option-set uid (exact)
    @Query(value = """
        SELECT ov.* FROM option_value ov
        JOIN option_set os ON ov.option_set_id = os.id
        WHERE os.uid = :setUid
          AND ov.name = :name
        LIMIT 1
        """, nativeQuery = true)
    Optional<Option> findByNameAndOptionSetUid(@Param("name") String name, @Param("setUid") String optionSetUid);

    // translations exact match (scoped by option_set.uid)
    @Query(value = """
        SELECT ov.* FROM option_value ov
        JOIN option_set os ON ov.option_set_id = os.id
        WHERE os.uid = :setUid
          AND EXISTS (
            SELECT 1 FROM jsonb_array_elements(ov.translations::jsonb) t
            WHERE (t ->> 'value') = :value
          )
        LIMIT 1
        """, nativeQuery = true)
    Optional<Option> findByTranslationValueExact(@Param("setUid") String optionSetUid, @Param("value") String value);

    // translations ilike / partial (scoped)
    @Query(value = """
        SELECT ov.* FROM option_value ov
        JOIN option_set os ON ov.option_set_id = os.id
        WHERE os.uid = :setUid
          AND EXISTS (
            SELECT 1 FROM jsonb_array_elements(ov.translations::jsonb) t
            WHERE (t ->> 'value') ILIKE :valuePattern
          )
        LIMIT 1
        """, nativeQuery = true)
    Optional<Option> findByTranslationValueILike(@Param("setUid") String optionSetUid, @Param("valuePattern") String valuePattern);
}
