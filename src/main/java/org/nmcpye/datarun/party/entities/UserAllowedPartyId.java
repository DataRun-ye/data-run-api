package org.nmcpye.datarun.party.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAllowedPartyId implements Serializable {
    @Column(name = "user_id", nullable = false, length = 26)
    private String userId;

    @Column(name = "party_id", nullable = false)
    private UUID partyId;
}
