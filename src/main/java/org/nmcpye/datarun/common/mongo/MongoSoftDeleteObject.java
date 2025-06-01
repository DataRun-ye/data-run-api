package org.nmcpye.datarun.common.mongo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.SoftDeleteObject;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public abstract class MongoSoftDeleteObject
        extends MongoAuditableBaseObject
        implements SoftDeleteObject<String> {
    protected Boolean deleted = false;

    public abstract Boolean getDeleted();

    public abstract void setDeleted(Boolean deleted);

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
        MongoSoftDeleteObject that = (MongoSoftDeleteObject) o;
        return deleted == that.deleted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), deleted);
    }
}
