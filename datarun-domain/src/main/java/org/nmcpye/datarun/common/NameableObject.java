package org.nmcpye.datarun.common;

public interface NameableObject<ID> extends IdentifiableObject<ID> {
    String getShortName();

    String getDisplayShortName();

    String getDescription();

    String getDisplayDescription();

    String getDisplayProperty(DisplayProperty property);
}
