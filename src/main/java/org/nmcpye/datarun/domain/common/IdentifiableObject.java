package org.nmcpye.datarun.domain.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    String getName();

    String getDisplayName();

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
