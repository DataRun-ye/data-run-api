package org.nmcpye.datarun.drun.postgres.common;

public interface NameableObject<T> extends IdentifiableEntity<T> {
    String getName();

    String getCode();
}
