package org.nmcpye.datarun.jpa.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Base abstract class for entities which will hold definitions for created, last modified, created by,
 * last modified by attributes.
 *
 * @author Hamza Assada, 20/03/2025
 */
@MappedSuperclass
@JsonIgnoreProperties(value = {"new", "createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate"}, allowGetters = true)
@Getter
@Setter
@NoArgsConstructor
public abstract class JpaAuditable<ID>
        implements Persistable<ID>, Serializable {

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
    @JsonIgnore
    public boolean isNew() {
        return !this.isPersisted;
    }

    @SuppressWarnings("UnusedReturnValue")
    public JpaAuditable<ID> setIsPersisted() {
        this.isPersisted = true;
        return this;
    }
}
