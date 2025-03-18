package org.nmcpye.datarun.drun.postgres.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface IdentifiableEntity<ID> extends Identifiable {

    ID getId();

    void setId(ID id);

    String getCreatedBy();

    void setCreatedBy(String user);

    @JsonIgnore
    default IdentifiableEntity<ID> setIsPersisted() {
        return null;
    }
}
