package org.nmcpye.datarun.party.resolution.strategies;

import org.nmcpye.datarun.party.dto.PartyResolutionRequest;
import org.nmcpye.datarun.party.dto.ResolvedParty;
import org.nmcpye.datarun.party.entities.PartySetKind;

import java.util.List;
import java.util.UUID;

/**
 * @author Hamza Assada 29/12/2025
 */
public interface PartySetStrategy {

    /**
     * @return The specific kind this strategy handles (STATIC, ORG_TREE, etc.)
     */
    PartySetKind getKind();

    /**
     * Resolves parties based on the party set specification.
     *
     * @param partySetId The ID of the configuration row (party_set)
     * @param spec       The JSONB spec payload (parsed or raw)
     * @param request    The user's request context
     * @return A list of resolved parties
     */
    List<ResolvedParty> resolve(UUID partySetId,
                                String spec,
                                boolean isMaterialized,
                                PartyResolutionRequest request);
}
