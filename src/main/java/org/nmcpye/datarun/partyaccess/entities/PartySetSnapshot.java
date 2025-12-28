package org.nmcpye.datarun.partyaccess.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.Instant;

/// @author Hamza Assada 28/12/2025
@Entity
@Table(name = "party_set_snapshot",
    indexes = {@Index(name = "idx_pss_party_set", columnList = "party_set_id")})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartySetSnapshot {
    @Id
    @Column(name = "id", length = 26, updatable = false, nullable = false)
    protected String id;

    @Column(length = 11, unique = true, nullable = false)
    private String uid;

    @Column(name = "party_set_id", length = 26, nullable = false)
    protected String partySetId;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String snapshot;

    @Column(name = "created_date", updatable = false, nullable = false)
    private Instant createdDate;
}
