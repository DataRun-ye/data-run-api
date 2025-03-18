package org.nmcpye.datarun.drun.postgres.common;

import org.nmcpye.datarun.utils.CodeGenerator;

public interface Identifiable {
    /**
     * @return external unique ID of the object as used in the RESTful API
     */
    String getUid();

    void setUid(String uid);

    default void setAutoFields() {
        if (getUid() == null || getUid().isEmpty()) {
            setUid(CodeGenerator.generateUid());
        }
    }

}
