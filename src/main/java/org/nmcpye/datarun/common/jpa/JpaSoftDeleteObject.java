package org.nmcpye.datarun.common.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.SoftDeleteObject;

import java.util.Objects;

/**
 * Base abstract class for entities which will hold definitions for created, last modified, created by,
 * last modified by attributes.
 *
 * @author Hamza Assada, 20/03/2025
 */
@MappedSuperclass
@Getter
@Setter
public abstract class JpaSoftDeleteObject extends JpaBaseIdentifiableObject
    implements SoftDeleteObject<Long> {

    @Column(name = "deleted")
    protected Boolean deleted = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        JpaSoftDeleteObject that = (JpaSoftDeleteObject) o;
        return deleted == that.deleted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), deleted);
    }
}
