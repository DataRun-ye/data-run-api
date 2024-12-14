package org.nmcpye.datarun.drun.postgres.common;

public interface NameableObject<T> extends Identifiable<T> {
    String getName();
    String getCode();
}
