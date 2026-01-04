package org.nmcpye.datarun.party.repository;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.nmcpye.datarun.party.entities.UserAllowedParty;
import org.nmcpye.datarun.party.entities.UserAllowedPartyId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UserAllowedPartyRepository
    extends BaseJpaRepository<UserAllowedParty, UserAllowedPartyId> {
    List<UserAllowedParty> findAll();

    Page<UserAllowedParty> findAll(Pageable pageable);

    Page<UserAllowedParty> findById_UserId(String idUserId, Pageable pageable);

    Page<UserAllowedParty> findById_UserIdAndLastUpdatedAfter(String idUserId, Instant lastUpdatedAfter, Pageable pageable);
}
