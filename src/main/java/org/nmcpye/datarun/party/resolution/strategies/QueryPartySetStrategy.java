package org.nmcpye.datarun.party.resolution.strategies;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.nmcpye.datarun.party.dto.PartyResolutionRequest;
import org.nmcpye.datarun.party.dto.ResolvedParty;
import org.nmcpye.datarun.party.entities.PartySetKind;
import org.nmcpye.datarun.party.resolution.engine.PartySecurityFilter;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.nmcpye.datarun.jooq.public_.Tables.PARTY;

@Component
public class QueryPartySetStrategy implements PartySetStrategy {

    private final NamedQueryProvider queryProvider;
    private final PartySecurityFilter securityFilter; // Injected
    private final ObjectMapper objectMapper;

    public QueryPartySetStrategy(NamedQueryProvider queryProvider, PartySecurityFilter securityFilter, ObjectMapper objectMapper) {
        this.queryProvider = queryProvider;
        this.securityFilter = securityFilter;
        this.objectMapper = objectMapper;
    }

    @Override
    public PartySetKind getKind() {
        return PartySetKind.QUERY;
    }

    @Override
    public List<ResolvedParty> resolve(UUID partySetId,
                                       String specJson,
                                       boolean isMaterialized,
                                       PartyResolutionRequest request) {
        try {
            // 1. Parse Spec
            // Expected Spec: { "query": "FIND_ORG_CHILDREN", "params": { "parentId": "region_id" } }
            JsonNode spec = objectMapper.readTree(specJson);
            String queryName = spec.path("sqlKey").asText();
            JsonNode paramMapping = spec.path("params");

            // 2. Resolve Parameters (Map Spec Param -> Context Value)
            Map<String, Object> resolvedParams = new HashMap<>();
            if (paramMapping.isObject()) {
                paramMapping.fields().forEachRemaining(entry -> {
                    String sqlParamName = entry.getKey();
                    String contextKey = entry.getValue().asText();

                    // Look up value in the Request Context (e.g. what the user selected in dropdown A)
                    Object contextValue = request.getContextValues().get(contextKey);
                    resolvedParams.put(sqlParamName, contextValue);
                });
            }

            // 3. Get Base Query
            SelectConditionStep<?> baseQuery = queryProvider.getQuery(queryName, resolvedParams);

            if (baseQuery == null) {
                // Pre-requisites not met (e.g. parent dropdown not selected yet)
                return Collections.emptyList();
            }

            // 4. Apply Common Filters (Search q)
            if (request.getSearchQuery() != null && !request.getSearchQuery().isBlank()) {
                String term = "%" + request.getSearchQuery().trim() + "%";
                baseQuery.and(
                    PARTY.NAME.likeIgnoreCase(term).or(PARTY.CODE.likeIgnoreCase(term))
                );
            }

            // --- APPLY SECURITY FILTER HERE ---
            // Note: The baseQuery MUST select from PARTY (or alias it correctly) for the filter to work.
            // The NamedQueryProvider should ensure queries are rooted in PARTY.
            var securedQuery = securityFilter.apply(baseQuery, request.getUserId(), isMaterialized);

            // 5. Execute & Map
            // Note: baseQuery is selecting *, so we can fetch into ResolvedParty manually
            return securedQuery
                .orderBy(PARTY.NAME.asc())
                .limit(request.getLimit())
                .offset(request.getOffset())
                .fetch(this::mapRecord);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Invalid JSON spec for PartySet " + partySetId, e);
        }
    }

    private ResolvedParty mapRecord(Record r) {
        // Safe mapping from the PARTY table columns in the result
        return new ResolvedParty(
            r.get(PARTY.ID),
            r.get(PARTY.UID),
            r.get(PARTY.TYPE),
            r.get(PARTY.NAME),
            r.get(PARTY.CODE),
            null, // json properties
            r.get(PARTY.SOURCE_TYPE)
        );
    }
}
