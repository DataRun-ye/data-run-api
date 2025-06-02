package org.nmcpye.datarun.mongo.common;

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
        return getDeleted() == that.getDeleted();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDeleted());
    }
}
