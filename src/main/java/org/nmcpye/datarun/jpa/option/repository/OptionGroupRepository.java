package org.nmcpye.datarun.jpa.option.repository;

import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.option.OptionGroup;
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
@Repository
public interface OptionGroupRepository
        extends JpaIdentifiableRepository<OptionGroup> {
    @Override
    default void deleteByUid(String uid) {
    }

    @Override
    default Optional<OptionGroup> findByUid(String uid, CurrentUserDetails user) {
        return Optional.empty();
    }

    @Override
    default Optional<OptionGroup> findByUid(String uid) {
        return Optional.empty();
    }

    @Override
    default Page<OptionGroup> findAllByUidIn(Collection<String> uids, Pageable pageable) {
        return Page.empty();
    }

    default void deleteAllByUidIn(Collection<String> uids) {

    }

    default Boolean existsByUid(@Size(max = 11) String uid) {
        return false;
    }

    default Optional<OptionGroup> findByIdOrUid(@Size(max = 26) String id, @Size(max = 11) String uid) {
        return Optional.empty();
    }

    @Override
    default List<OptionGroup> findDistinctByIdInOrUidIn(Collection<String> ids, Collection<String> uids){
        return Collections.emptyList();
    }

    default List<OptionGroup> findAllByUidIn(Collection<String> uids) {
        return Collections.emptyList();
    }
}
