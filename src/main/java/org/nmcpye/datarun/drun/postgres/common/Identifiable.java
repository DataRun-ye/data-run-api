package org.nmcpye.datarun.drun.postgres.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Identifiable<ID> extends SystemIdentifiable {

    void setId(ID id);

    ID getId();

//    /**
//     * @return external unique ID of the object as used in the RESTful API
//     */
//    String getUid();
//
//    void setUid(String uid);

    String getCreatedBy();
    void setCreatedBy(String user);

    @JsonIgnore
    default Identifiable<ID> setIsPersisted() {
        return null;
    }
}
