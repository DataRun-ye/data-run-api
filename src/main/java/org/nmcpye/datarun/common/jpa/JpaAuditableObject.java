package org.nmcpye.datarun.common.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Base abstract class for entities which will hold definitions for created, last modified, created by,
 * last modified by attributes.
 *
 * @author Hamza, 20/03/2025
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(value = {"createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate"}, allowGetters = true)
@Getter
@Setter
public abstract class JpaAuditableObject
    implements AuditableObject<Long>, Persistable<Long>, Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    protected Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    protected String uid;

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

    public JpaAuditableObject() {
        setAutoFields();
    }

    @Transient
    protected boolean isPersisted;

    @PostLoad
    @PostPersist
    public void updateEntityState() {
        this.setIsPersisted();
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
    public boolean isNew() {
        return !this.isPersisted;
    }

    public JpaAuditableObject setIsPersisted() {
        this.isPersisted = true;
        return this;
    }

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
        if (this == o) return true;
        if (!(o instanceof JpaAuditableObject that)) return false;
        return Objects.equals(getUid(), that.getUid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUid());
    }
}
