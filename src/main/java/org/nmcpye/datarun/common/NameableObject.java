package org.nmcpye.datarun.common;

import org.nmcpye.datarun.drun.postgres.common.IdentifiableEntity;

public interface NameableObject<T> extends IdentifiableEntity<T> {
    String getName();

    String getCode();

    String getShortName();

    String getDisplayShortName();

    String getDescription();
}
