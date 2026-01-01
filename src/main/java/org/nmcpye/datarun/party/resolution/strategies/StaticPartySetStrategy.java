package org.nmcpye.datarun.party.resolution.strategies;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.nmcpye.datarun.party.dto.PartyResolutionRequest;
import org.nmcpye.datarun.party.dto.ResolvedParty;
import org.nmcpye.datarun.party.entities.PartySetKind;
import org.nmcpye.datarun.party.resolution.engine.PartySecurityFilter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static org.nmcpye.datarun.jooq.public_.tables.Party.PARTY;
import static org.nmcpye.datarun.jooq.public_.tables.PartySetMember.PARTY_SET_MEMBER;

/**
 * @author Hamza Assada 29/12/2025
 */
@Component
public class StaticPartySetStrategy implements PartySetStrategy {

    private final DSLContext dsl;
    private final PartySecurityFilter securityFilter; // Injected

    public StaticPartySetStrategy(DSLContext dsl, PartySecurityFilter securityFilter) {
        this.dsl = dsl;
        this.securityFilter = securityFilter;
    }

    @Override
    public PartySetKind getKind() {
        return PartySetKind.STATIC;
    }

    @Override
    public List<ResolvedParty> resolve(UUID partySetId,
                                       String spec,
                                       boolean isMaterialized,
                                       PartyResolutionRequest request) {

        // 1. Base Condition: Must belong to this specific set
        Condition whereCondition = PARTY_SET_MEMBER.PARTY_SET_ID.eq(partySetId);

        // 2. Apply Search (if provided)
        // We check name or code via case-insensitive search
        if (request.getSearchQuery() != null && !request.getSearchQuery().isBlank()) {
            String term = "%" + request.getSearchQuery().trim() + "%";
            whereCondition = whereCondition.and(
                PARTY.NAME.likeIgnoreCase(term)
                    .or(PARTY.CODE.likeIgnoreCase(term))
            );
        }

        var query = dsl.select(
                PARTY.ID,
                PARTY.UID,
                PARTY.TYPE,
                PARTY.NAME,
                PARTY.CODE,
                PARTY.PROPERTIES_MAP,
                PARTY.SOURCE_TYPE // Assuming you added this to distinguish Internal vs External
            )
            .from(PARTY)
            .join(PARTY_SET_MEMBER).on(PARTY.ID.eq(PARTY_SET_MEMBER.PARTY_ID))
            .where(whereCondition);

        // --- APPLY SECURITY FILTER ---
        var securedQuery = securityFilter.apply(query, request.getUserId(), isMaterialized);

        // 3. Execute Query
        return securedQuery
            .orderBy(PARTY.NAME.asc())
            .limit(request.getLimit())
            .offset(request.getOffset())
            .fetch(record -> new ResolvedParty(
                record.get(PARTY.ID),
                record.get(PARTY.UID),
                record.get(PARTY.TYPE),
                record.get(PARTY.NAME), // Mapping 'name' to 'label'
                record.get(PARTY.CODE),
                // Safely handle JSONB -> Map conversion if needed, or pass null
                // jOOQ usually maps JSONB to a JSON object or String, depending on config.
                // Assuming implicit conversion or a utility method here:
                null,
                record.get(PARTY.SOURCE_TYPE)
            ));
    }
}
