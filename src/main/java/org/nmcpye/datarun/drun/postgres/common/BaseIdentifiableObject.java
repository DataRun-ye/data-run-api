package org.nmcpye.datarun.drun.postgres.common;

import jakarta.persistence.MappedSuperclass;
import org.nmcpye.datarun.domain.AbstractAuditingEntity;

import static org.nmcpye.datarun.drun.postgres.hibernate.HibernateProxyUtils.getRealClass;

@MappedSuperclass
public abstract class BaseIdentifiableObject<T>
    extends AbstractAuditingEntity<T>
    implements IdentifiableObject<T> {

    //    public BaseIdentifiableObject() {
//      setAutoFields();
//    }
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
