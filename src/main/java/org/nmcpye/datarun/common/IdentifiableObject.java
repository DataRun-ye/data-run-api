package org.nmcpye.datarun.common;

import org.nmcpye.datarun.common.translation.Translation;

import java.util.Set;

/**
 * @author Hamza Assada 20/03/2025 <7amza.it@gmail.com>
 */
public interface IdentifiableObject<ID>
    extends AuditableObject<ID>, Comparable<IdentifiableObject<ID>> {
    ID getId();

    void setAutoFields();
    String getCode();

    String getName();

    String getDisplayName();

    Set<Translation> getTranslations();
}
