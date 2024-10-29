package org.nmcpye.datarun.drun.postgres.common;

public interface Identifiable<ID> {

    void setId(ID id);

    ID getId();

    /**
     * @return external unique ID of the object as used in the RESTful API
     */
    String getUid();

    void setUid(String uid);
}
