package org.nmcpye.datarun.common;

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

    /**
     * Returns the value of the property referred to by the given IdScheme.
     *
     * @param idScheme the IdScheme.
     * @return the value of the property referred to by the IdScheme.
     */
    @Override
    default String getPropertyValue(IdScheme idScheme) {
        if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
            return getUid();
        } else if (idScheme.is(IdentifiableProperty.CODE)) {
            return getCode();
        } else if (idScheme.is(IdentifiableProperty.NAME)) {
            return getName();
        }

        return null;
    }
}
