package org.nmcpye.datarun.party.resolution.engine;

import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import static org.nmcpye.datarun.jooq.public_.Tables.PARTY;
import static org.nmcpye.datarun.jooq.public_.Tables.USER_ALLOWED_PARTY;

@Component
public class PartySecurityFilter {

    /**
     * Applies the permission filter to an ongoing query.
     *
     * @param query          The base query (selecting from PARTY or joining PARTY)
     * @param userId         The requesting user
     * @param isMaterialized If false, the set is Public/Unsecured, so we skip the check.
     * @return The query with the security JOIN/WHERE clause appended
     */
    public <R extends org.jooq.Record> SelectConditionStep<R> apply(
        SelectConditionStep<R> query,
        String userId,
        boolean isMaterialized
    ) {
        if (!isMaterialized) {
            // Public set (e.g., Generic Option List, Survey Locations) -> No security check needed
            return query;
        }

        // Secure set: strict INNER JOIN to user_allowed_party
        // This automatically filters out any party not present in the permission table for this user.
        return query
            .andExists(
                DSL.selectOne()
                    .from(USER_ALLOWED_PARTY)
                    .where(USER_ALLOWED_PARTY.PARTY_ID.eq(PARTY.ID))
                    .and(USER_ALLOWED_PARTY.USER_ID.eq(userId))
            );
    }
}
