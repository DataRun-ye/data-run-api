package org.nmcpye.datarun.domain.common;

public interface IdentifiableObject<ID> {

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

    default IdentifiableObject<ID> setIsPersisted() {
        return null;
    }
}
