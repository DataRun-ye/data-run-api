package org.nmcpye.datarun.partyaccess.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import java.time.Instant;

@Entity
@Table(name = "party_set")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
public class PartySet {
    @Id
    @Column(name = "id", length = 26, updatable = false, nullable = false)
    protected String id;

    @Column(length = 11, unique = true, nullable = false)
    private String uid; // 11-char Business Key

    @Column(nullable = false)
    private String name;

    @Column(length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private PartySetKind kind; // STATIC, ORG_TREE, TAG_FILTER, etc.

    @Column(columnDefinition = "jsonb default '{}'::jsonb")
    @Type(JsonType.class)
    private PartySetSpec spec;

    @Column(name = "created_date", updatable = false, nullable = false)
    private Instant createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private Instant lastModifiedDate;
}
