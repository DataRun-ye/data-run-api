package org.nmcpye.datarun.party.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.party.dto.PagedRequest;
import org.nmcpye.datarun.party.dto.UserAllowedPartyDto;
import org.nmcpye.datarun.party.entities.UserAllowedParty;
import org.nmcpye.datarun.party.mapper.UserAllowedPartyMapper;
import org.nmcpye.datarun.party.repository.UserAllowedPartyRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPermissionSyncService {
    private final UserAllowedPartyRepository userAllowedPartyRepository;
    private final UserAllowedPartyMapper userAllowedPartyMapper;

    public Page<UserAllowedPartyDto> getAllowedPartiesForUser(String userId, PagedRequest pageable) {

        Page<UserAllowedParty> allowedParties;
        if (userId != null) {
            allowedParties = pageable.getSince() != null ?
                userAllowedPartyRepository.findById_UserIdAndLastUpdatedAfter(userId, pageable.getSince(), pageable.getPageable()) :
                userAllowedPartyRepository.findById_UserId(userId, pageable.getPageable());
        } else {
            allowedParties = userAllowedPartyRepository.findAll(pageable.getPageable());
        }

        return allowedParties.map(userAllowedPartyMapper::toDto);
    }
}
