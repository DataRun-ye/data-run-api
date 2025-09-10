package org.nmcpye.datarun.jpa.option.repository;

import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.option.Option;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(cacheNames = OPTIONS_OPTION_SET_UID)
    List<Option> findAllByOptionSetUid(@Size(max = 11) String optionSetUid);

    Optional<Option> findByCodeAndOptionSetUid(String code, @Size(max = 11) String optionSetUid);

    // no-op
    @Override
    default void deleteByUid(String uid) {
    }

    default void deleteAllByUidIn(Collection<String> uids) {
    }
}
