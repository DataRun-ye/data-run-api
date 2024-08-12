package org.nmcpye.datarun.drun.mongo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.nmcpye.datarun.domain.common.IdentifiableObject;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.Instant;

/**
 * Base abstract class for entities which will hold definitions for created, last modified, created by,
 * last modified by attributes.
 */
@JsonIgnoreProperties(value = {"createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate"}, allowGetters = true)
@DiffIgnore
public abstract class AbstractAuditingEntityMongo<T>
    implements IdentifiableObject<T>, Serializable {

    private static final long serialVersionUID = 1L;

    public abstract T getId();

    @CreatedBy
    @Field("created_by")
    private String createdBy;

    @CreatedDate
    @Field("created_date")
    private Instant createdDate = Instant.now();

    @LastModifiedBy
    @Field("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Field("last_modified_date")
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
        int result = getUid() != null ? getUid().hashCode() : 0;
        result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
//        result = 31 * result + (getUid() != null ? getName().hashCode() : 0);

        return result;
    }

    @Override
    public String getDisplayName() {
//        return getTranslation("NAME", getName());
        return getName();
    }

    /**
     * Compares objects based on display name. A null display name is ordered
     * after a non-null display name.
     */
    @Override
    public int compareTo(IdentifiableObject<T> object) {
        if (this.getDisplayName() == null && object != null) {
            return object.getDisplayName() == null ? 0 : 1;
        }

        return object == null || object.getDisplayName() == null ? -1 : this.getDisplayName().compareToIgnoreCase(object.getDisplayName());
    }

    /**
     * Equality check against typed identifiable object. This method is not
     * vulnerable to proxy issues, where an uninitialized object class type
     * fails comparison to a real class.
     *
     * @param other the identifiable object to compare this object against.
     * @return true if equal.
     */
    public boolean typedEquals(IdentifiableObject<T> other) {
        if (other == null) {
            return false;
        }

        if (getUid() != null ? !getUid().equals(other.getUid()) : other.getUid() != null) {
            return false;
        }

        if (getCode() != null ? !getCode().equals(other.getCode()) : other.getCode() != null) {
            return false;
        }

//        if (getName() != null ? !getName().equals(other.getName()) : other.getName() != null) {
//            return false;
//        }

        return true;
    }
}
