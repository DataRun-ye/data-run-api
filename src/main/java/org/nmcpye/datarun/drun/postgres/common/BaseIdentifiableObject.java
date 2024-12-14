package org.nmcpye.datarun.drun.postgres.common;

import jakarta.persistence.MappedSuperclass;
import org.nmcpye.datarun.domain.AbstractAuditingEntity;
import org.nmcpye.datarun.utils.CodeGenerator;

import java.time.Instant;

import static org.nmcpye.datarun.drun.postgres.hibernate.HibernateProxyUtils.getRealClass;

@MappedSuperclass
public abstract class BaseIdentifiableObject<T>
    extends AbstractAuditingEntity<T>
    implements IdentifiableObject<T> {

//    @Type(JsonType.class)
//    @Column(name = "metadata", columnDefinition = "jsonb")
//    protected Map<String, Object> metadata = new HashMap<>();
//
//    @JsonProperty
//    public Map<String, Object> getMetadata() {
//
//        if (metadata == null) {
//            metadata = new HashMap<>();
//        }
//
//        return metadata;
//    }
//
//    /**
//     * Clears out cache when setting metadata.
//     */
//    public void setMetadata(Map<String, Object> metadata) {
//
//        this.metadata = metadata;
//    }
//

    // -------------------------------------------------------------------------
    // hashCode and equals
    // -------------------------------------------------------------------------
    @Override
    public int hashCode() {
        int result = getUid() != null ? getUid().hashCode() : 0;
        return result;
    }

    /**
     * Class check uses isAssignableFrom and get-methods to handle proxied
     * objects.
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof BaseIdentifiableObject
            && getRealClass(this) == getRealClass(obj)
            && typedEquals((IdentifiableObject<T>) obj);
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
        return true;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------


    /**
     * Set auto-generated fields on save or update
     */
    public void setAutoFields() {
        if (getUid() == null || getUid().isEmpty()) {
            setUid(CodeGenerator.generateUid());
        }

        Instant date = Instant.now();
    }

    @Override
    public String toString() {
        return (
            "{" +
                "\"class\":\"" +
                getClass() +
                "\", " +
                "\"id\":\"" +
                getId() +
                "\", " +
                "\"uid\":\"" +
                getUid() +
                "\", " +
                "\", " +
                "\", " +
                "\"created\":\"" +
                "\" " +
                "}"
        );
    }
}
