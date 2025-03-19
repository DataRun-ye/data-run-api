package org.nmcpye.datarun.drun.postgres.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

import static org.nmcpye.datarun.drun.postgres.hibernate.HibernateProxyUtils.getRealClass;

/**
 * Base abstract class for entities which will hold definitions for created, last modified, created by,
 * last modified by attributes.
 */
@MappedSuperclass
@JsonIgnoreProperties(value = {"createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate"}, allowGetters = true)
public abstract class AuditingEntity
    implements PrimaryKeyLongObject {

    private static final long serialVersionUID = 1L;

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private Instant createdDate = Instant.now();

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate = Instant.now();

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    // -------------------------------------------------------------------------
    // hashCode and equals
    // -------------------------------------------------------------------------


    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getUid() != null ? getUid().hashCode() : 0);

        return result;
    }

    /**
     * Class check uses isAssignableFrom and get-methods to handle proxied
     * objects.
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof AuditingEntity
            && getRealClass(this) == getRealClass(obj)
            && typedEquals((PrimaryKeyLongObject) obj);
    }

    /**
     * Equality check against typed identifiable object. This method is not
     * vulnerable to proxy issues, where an uninitialized object class type
     * fails comparison to a real class.
     *
     * @param other the identifiable object to compare this object against.
     * @return true if equal.
     */
    public boolean typedEquals(PrimaryKeyLongObject other) {
        if (other == null) {
            return false;
        }

        if (getId() != null ? !getId().equals(other.getId()) : other.getId() != null) {
            return false;
        }

        if (getUid() != null ? !getUid().equals(other.getUid()) : other.getUid() != null) {
            return false;
        }
        return true;
    }
}
