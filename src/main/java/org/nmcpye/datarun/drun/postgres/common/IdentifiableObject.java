package org.nmcpye.datarun.drun.postgres.common;

import java.io.Serializable;

public interface IdentifiableObject<ID>
    extends IdentifiableEntity<ID>,/* Comparable<IdentifiableObject<ID>>,*/ Serializable {

    String getName();
}
