package org.nmcpye.datarun.domainmapping.service;

/**
 * @author Hamza Assada
 * @since 23/09/2025
 */
public interface LookupService {
    /**
     * Resolve option code -> option_uid (canonical). Return null if not found.
     */
    String resolveOptionValueUid(String optionSetUid, String optionCode);

    /**
     * Resolve external org unit id -> org_unit uid, or null.
     */
    String resolveOrgUnitUid(String externalId);

    // Add other lookups (team, activity) as needed.
}
