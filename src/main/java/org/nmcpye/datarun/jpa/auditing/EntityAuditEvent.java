package org.nmcpye.datarun.jpa.auditing;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "app_entity_audit_event",
    indexes = {
        @Index(name = "idx_entity_audit_event_entity_type", columnList = "entity_type"),
//        @Index(name = "idx_entity_audit_event_entityid_version", columnList = "entity_type, entity_id, commit_version")
    })
@Setter
@Getter
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class EntityAuditEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Column(name = "entity_Id", length = 26, nullable = false)
    private String entityId;

    @NotNull
    @Size(max = 2500)
    @Column(name = "entity_type", length = 2500, nullable = false)
    private String entityType;

    @NotNull
    @Size(max = 20)
    @Column(name = "action", length = 20, nullable = false)
    private String action;

    @Size(max = 50000)
    @Column(name = "entity_value", length = 50000)
    private String entityValue;
//
//    @Column(name = "the_value", columnDefinition = "jsonb")
//    @JdbcTypeCode(SqlTypes.JSON)
//    private JsonNode theValue;

//    @Type(JsonType.class)
//    @Column(name = "entity_value", columnDefinition = "jsonb")
//    private Map<String, Object> entityValue;

    @Column(name = "commit_version")
    private Integer commitVersion;

    @Size(max = 100)
    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @NotNull
    @Column(name = "modified_date", nullable = false)
    private Instant modifiedDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EntityAuditEvent entityAuditEvent = (EntityAuditEvent) o;
        return Objects.equals(id, entityAuditEvent.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return (
            "EntityAuditEvent{" +
                "id=" +
                id +
                ", entityId='" +
                entityId +
                "'" +
                ", entityType='" +
                entityType +
                "'" +
                ", action='" +
                action +
                "'" +
                ", entityValue='" +
                entityValue +
                "'" +
                ", commitVersion='" +
                commitVersion +
                "'" +
                ", modifiedBy='" +
                modifiedBy +
                "'" +
                ", modifiedDate='" +
                modifiedDate +
                "'" +
                '}'
        );
    }
}
