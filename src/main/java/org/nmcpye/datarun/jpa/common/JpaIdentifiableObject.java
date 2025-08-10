package org.nmcpye.datarun.jpa.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.nmcpye.datarun.common.IdentifiableObject;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Base abstract class for entities which will hold definitions for created, last modified, created by,
 * last modified by attributes.
 *
 * @author Hamza Assada 20/03/2025 (7amza.it@gmail.com)
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(value = {"new", "createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate"}, allowGetters = true)
@DiffIgnore
@Getter
@Setter
public abstract class JpaIdentifiableObject
    implements IdentifiableObject<String>, Persistable<String>, Serializable {
    /**
     * ULID id (Universally Unique Lexicographically Sortable Identifier).
     * Length: 26 characters, Base32 encoded.
     */
    @Id
    @Column(name = "id", length = 26, updatable = false, nullable = false)
    protected String id;

    @Serial
    private static final long serialVersionUID = 1L;

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    protected String createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    protected Instant createdDate = Instant.now();

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 50)
    protected String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    protected Instant lastModifiedDate = Instant.now();

    @Transient
    protected boolean isPersisted;

    public JpaIdentifiableObject() {
        setAutoFields();
    }

    @PostLoad
    @PostPersist
    protected void updateEntityState() {
        this.setIsPersisted();
    }

    /**
     * Lifecycle hook to generate a ULID ID before persisting.
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            setId(CodeGenerator.ULIDGenerator.nextString());
        }
    }

    @JsonIgnore
    public boolean isPersisted() {
        return isPersisted;
    }

    public void setPersisted(boolean persisted) {
        isPersisted = persisted;
    }

    @Transient
    @Override
    @JsonIgnore
    public boolean isNew() {
        return !this.isPersisted;
    }

    public JpaIdentifiableObject setIsPersisted() {
        this.isPersisted = true;
        return this;
    }

    abstract protected void setUid(String uid);

    /**
     * Set auto-generated fields on save or update
     */
    public void setAutoFields() {
        if (getUid() == null || getUid().isEmpty()) {
            setUid(CodeGenerator.generateUid());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JpaIdentifiableObject that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
