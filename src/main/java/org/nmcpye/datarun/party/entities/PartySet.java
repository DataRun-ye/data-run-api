package org.nmcpye.datarun.party.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.translation.Translation;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.jpa.common.TranslatableInterface;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.*;

/// @author Hamza Assada 28/12/2025
@Entity
@Table(name = "party_set")
@EntityListeners(AuditingEntityListener.class)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartySet implements TranslatableInterface, Persistable<UUID> {

    @Data
    @Builder
    public static class PartySetSpec {
        private UUID rootId;              // For ORG_TREE
        private Integer depth;              // nullable For ORG_TREE
        private Boolean includeSelf;        // For ORG_TREE

        private List<String> tags;          // For TAG_FILTER
        private List<String> types;         // ORG_UNIT, TEAM, USER. Nullable For Specific TAG_FILTER

        private String sqlKey;              // For QUERY
        private Map<String, Object> params; // For QUERY
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(length = 11, unique = true, nullable = false)
    private String uid;

    @Column(name = "code", unique = true, length = 32)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private PartySetKind kind;

    @Column(columnDefinition = "jsonb default '{}'::jsonb", nullable = false)
    @Type(JsonType.class)
    private PartySetSpec spec;

    @Column(name = "is_materialized")
    @Builder.Default
    protected Boolean isMaterialized = Boolean.TRUE;

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    protected String createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false, nullable = false)
    protected Instant createdDate;

    @LastModifiedBy
    @Column(name = "last_modified_by", nullable = false, length = 50)
    protected String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date", nullable = false)
    protected Instant lastModifiedDate;

    @Type(JsonType.class)
    @Column(name = "translations", columnDefinition = "jsonb")
    @Builder.Default
    protected Set<Translation> translations = new HashSet<>();

    @Transient
    @JsonIgnore
    protected boolean isNew;

    public PartySet() {
        setAutoFields();
    }

    /**
     * Set auto-generated fields on save or update
     */
    public void setAutoFields() {
        if (getUid() == null || getUid().isEmpty()) {
            setUid(CodeGenerator.generateUid());
        }
    }

    @JsonIgnore
    public PartySet persisted() {
        isNew = true;
        return this;
    }
}
