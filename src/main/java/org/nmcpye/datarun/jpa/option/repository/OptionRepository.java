package org.nmcpye.datarun.jpa.option.repository;

import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.option.Option;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Hamza Assada 30/06/2025 (7amza.it@gmail.com)
 */
@SuppressWarnings("unused")
@Repository
public interface OptionRepository
        extends JpaIdentifiableRepository<Option>, OptionRepositoryCustom {
    // no-op
    @Override
    default void deleteByUid(String uid) {
    }

    @Override
    default Optional<Option> findByUid(String uid, CurrentUserDetails user) {
        return Optional.empty();
    }

    @Override
    default Optional<Option> findByUid(String uid) {
        return Optional.empty();
    }

    @Override
    default Page<Option> findAllByUidIn(Collection<String> uids, Pageable pageable) {
        return Page.empty();
    }

    default void deleteAllByUidIn(Collection<String> uids) {

    }

    default Boolean existsByUid(@Size(max = 11) String uid) {
        return false;
    }

    default Optional<Option> findByIdOrUid(@Size(max = 26) String id, @Size(max = 11) String uid) {
        return Optional.empty();
    }

    default List<Option> findAllByUidIn(Collection<String> uids) {
        return Collections.emptyList();
    }
}
