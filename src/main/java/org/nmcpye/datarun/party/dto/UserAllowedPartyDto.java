package org.nmcpye.datarun.party.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link org.nmcpye.datarun.party.entities.UserAllowedParty}
 */
@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class UserAllowedPartyDto implements Serializable {
    private final String idUserId;
    private final UUID idPartyId;
    private final Integer permissionMask;
    private final Map<String, Object> provenance;
    private final Instant lastUpdated;
}
