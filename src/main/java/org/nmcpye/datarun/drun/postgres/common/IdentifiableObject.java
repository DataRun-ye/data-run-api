package org.nmcpye.datarun.drun.postgres.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.nmcpye.datarun.drun.postgres.common.translation.Translation;

import java.util.HashSet;
import java.util.Set;

public interface IdentifiableObject<ID>
    extends Comparable<IdentifiableObject<ID>> {

    void setId(ID id);

    ID getId();

    /**
     * @return external unique ID of the object as used in the RESTful API
     */
    String getUid();

    void setUid(String uid);

    default String getCode() {
        return null;
    }

    default void setCode(String code) {

    }

    default String getPropertyValue(IdScheme idScheme) {
        if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
            return getUid();
        } else if (idScheme.is(IdentifiableProperty.CODE)) {
            return getCode();
        } else if (idScheme.is(IdentifiableProperty.NAME)) {
            return getName();
        } else if (idScheme.is(IdentifiableProperty.ID)) {
            return String.valueOf(getId());
        }
        return null;
    }

    String getName();

    String getCreatedBy();

    String getDisplayName();

    default Set<Translation> getTranslations() {
        return new HashSet<>();
    }

    @JsonIgnore
    default IdentifiableObject<Long> setIsPersisted() {
        return null;
    }

//    default String getName() {
//        return null;
//    }
//
//    default void setName(String name) {
//
//    }

//    String getCreatedBy();
//
//    void setCreatedBy(String createdBy);
//
//    Instant getCreatedDate();
//
//    void setCreatedDate(Instant createdDate);
//
//    String getLastModifiedBy();
//
//    void setLastModifiedBy(String lastModifiedBy);
//
//    Instant getLastModifiedDate();
//
//    void setLastModifiedDate(Instant lastModifiedDate);
}
