package org.nmcpye.datarun.partyaccess.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.jpa.common.TranslatableIdentifiable;

import java.util.List;
import java.util.Map;

/// @author Hamza Assada 28/12/2025
@Entity
@Table(name = "party_set")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
public class PartySet extends TranslatableIdentifiable {
    @Column(length = 11, unique = true, nullable = false)
    private String uid; // 11-char Business Key

    @Column(name = "code", unique = true, length = 32)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private PartySetKind kind;

    @Column(columnDefinition = "jsonb default '{}'::jsonb")
    @Type(JsonType.class)
    private PartySetSpec spec;

    @Data
    public static class PartySetSpec {
        private List<String> members;       // For STATIC

        private String rootId;              // For ORG_TREE
        private Integer depth;              // For ORG_TREE
        private Boolean includeSelf;        // For ORG_TREE

        private List<String> tags;          // For TAG_FILTER
        private List<String> types;         // For TAG_FILTER

        private String sqlKey;              // For QUERY
        private Map<String, Object> params; // For QUERY
    }
}
