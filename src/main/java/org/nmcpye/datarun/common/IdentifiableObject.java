package org.nmcpye.datarun.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.nmcpye.datarun.common.translation.Translation;

import java.util.Set;

/**
 * @author Hamza Assada, 20/03/2025
 */
public interface IdentifiableObject<ID>
    extends AuditableObject<ID>, Comparable<IdentifiableObject<ID>> {
    ID getId();

    String getCode();

    String getName();

    String getDisplayName();

    Set<Translation> getTranslations();

    @JsonIgnore
    String getPropertyValue(IdScheme idScheme);
}
