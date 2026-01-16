package org.nmcpye.datarun.ledger;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.ledger.model.Submission;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Basic DB-backed mapping cache. Minimal TTL behavior; replace with Ehcache if needed.
 */
@Component
@RequiredArgsConstructor
public class DbMappingCache implements MappingCache {

    private final NamedParameterJdbcTemplate jdbc;

    // simple caches (in-memory). Could be replaced by Ehcache
    private volatile Map<String, String> skuCache = Map.of();
    private volatile long skuCacheAt = 0L;
    private final long ttlMs = 60_000L;

    @Override
    public String resolveSku(String categoryUid, String categoryText) {
        refreshSkuCacheIfStale();
        if (categoryUid != null && skuCache.containsKey(categoryUid)) return skuCache.get(categoryUid);
        // try fallback by text
        return skuCache.getOrDefault(categoryText, null);
    }

    private synchronized void refreshSkuCacheIfStale() {
        if (System.currentTimeMillis() - skuCacheAt < ttlMs) return;
        Map<String, String> map = jdbc.query("SELECT option_uid, sku_id FROM analytics.config_sku_mapping",
            (rs) -> {
                java.util.Map<String, String> m = new java.util.HashMap<>();
                while (rs.next()) {
                    m.put(rs.getString("option_uid"), rs.getString("sku_id"));
                }
                return m;
            });
        skuCache = map;
        skuCacheAt = System.currentTimeMillis();
    }

    @Override
    public void resolvePartiesForSubmission(Submission s) {
        // Rule set (same we discussed):
        // - HF_RECEIPT: to = orgUnitUid; from = assignment.wh_org_unit_uid (if available in supply_party_candidates) else team
        // - WH_TEAM_RECEIPT: to = team party; from = wh (assignment)
        // - WH_TEAM_RETURN / RETURN: from = team, to = wh assignment
        // - ISSUE/TRANSFER: from = assignment.wh_org_unit_uid or team wh; to = orgUnitUid

        // try to look up assignment's wh_org_unit_uid from supply_party_candidates (assignment properties)
        String assignmentUid = s.getAssignmentUid();
        // quick query to read adt.assignment_properties ->> 'wh_org_unit_uid' or 'wh_org_unit_uid' in supply_party_candidates
        String maybeWh = jdbc.queryForObject(
            "SELECT (assignment_properties::jsonb ->> 'wh_org_unit_uid') FROM analytics.dim_assignment WHERE assignment_uid = :aid",
            new MapSqlParameterSource("aid", assignmentUid),
            String.class);

        // lookup party ids in supply_party_candidates view by uid
        String orgUnitUid = s.getOrgUnitUid();
        String teamUid = s.getTeamUid();

        String toParty = null;
        String fromParty = null;

        switch ((s.getFlowType() == null ? "" : s.getFlowType())) {
            case "HF_RECEIPT":
                toParty = lookupPartyIdByUid(orgUnitUid);
                fromParty = maybeWh != null ? lookupPartyIdByUid(maybeWh) : lookupPartyIdByUid(teamUid);
                break;
            case "WH_TEAM_RECEIPT":
                toParty = lookupPartyIdByUid(teamUid);
                fromParty = maybeWh != null ? lookupPartyIdByUid(maybeWh) : lookupPartyIdByUid(teamUid);
                break;
            case "WH_TEAM_RETURN":
            case "HF_RETURN":
            case "RETURN":
                fromParty = lookupPartyIdByUid(teamUid);
                toParty = maybeWh != null ? lookupPartyIdByUid(maybeWh) : lookupPartyIdByUid(orgUnitUid);
                break;
            default:
                // default heuristics
                fromParty = maybeWh != null ? lookupPartyIdByUid(maybeWh) : lookupPartyIdByUid(teamUid);
                toParty = lookupPartyIdByUid(orgUnitUid);
                break;
        }

        s.setFromPartyId(fromParty);
        s.setToPartyId(toParty);
    }

    private String lookupPartyIdByUid(String uid) {
        if (uid == null) return null;
        try {
            return jdbc.queryForObject(
                "SELECT party_id FROM analytics.supply_party_candidates WHERE party_uid = :uid LIMIT 1",
                new MapSqlParameterSource("uid", uid),
                String.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
