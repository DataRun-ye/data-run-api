package org.nmcpye.datarun.party.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "user_allowed_party")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAllowedParty {
    @EmbeddedId
    private UserAllowedPartyId id;

    @Column(name = "permission_mask", nullable = false)
    private Integer permissionMask;

    /// Provenance: always store provenance in user_allowed_party
    /// row — `[{source:'assignment_binding', bindingId, principalType, principalId}, ...]`.
    @Type(JsonType.class)
    @Column(name = "provenance", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> provenance;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;
}
