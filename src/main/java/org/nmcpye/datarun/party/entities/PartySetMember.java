package org.nmcpye.datarun.party.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "party_set_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartySetMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "party_set_id", length = 26, nullable = false)
    private UUID partySetId;

    @Column(name = "party_id", length = 26, nullable = false)
    private UUID partyId;
}
