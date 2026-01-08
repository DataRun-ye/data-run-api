package org.nmcpye.datarun.party.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.v1.NamedObject;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/// @author Hamza Assada 28/12/2025
@Entity
@Table(name = "party")
@EntityListeners(AuditingEntityListener.class)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Party extends NamedObject {
    public enum PartyType {INTERNAL, EXTERNAL}

    public enum SourceType {ORG_UNIT, TEAM, USER, STATIC, EXTERNAL}

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /// 11-char Business Key source_uid
    @Column(name = "uid", length = 11, unique = true, nullable = false, updatable = false)
    private String uid;

    /// source code
    @Column(name = "code", length = 32)
    protected String code;

    /// source name
    @Column(name = "name", nullable = false)
    private String name;

    /// INTERNAL, EXTERNAL
    @Column(name = "type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private PartyType type;


    /// ORG_UNIT, TEAM, USER, STATIC, EXTERNAL Types
    @Column(name = "source_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    @Column(name = "source_id", length = 64, nullable = false)
    private String sourceId;

    /// for parties with parents such as orgUnits,
    ///  we use `orgUnit.id` which is the same as orgUnit's `party.source_id` of the parent org_unit
    @Column(name = "parent_id", length = 32)
    private UUID parentId;

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    protected String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 50)
    protected String lastModifiedBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false, nullable = false)
    protected Instant createdDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    protected Instant lastModifiedDate;
}
