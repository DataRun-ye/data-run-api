//package org.nmcpye.datarun.party.resolution;
//
//import lombok.RequiredArgsConstructor;
//import org.jooq.DSLContext;
//import org.nmcpye.datarun.party.dto.PartiesResponse;
//import org.nmcpye.datarun.party.dto.ResolvedParty;
//import org.nmcpye.datarun.party.entities.AssignmentPartyBinding;
//import org.nmcpye.datarun.party.entities.PartySet;
//import org.nmcpye.datarun.party.entities.PartySet.PartySetSpec;
//import org.nmcpye.datarun.party.entities.PartySetKind;
//import org.nmcpye.datarun.party.repository.PartySetRepository;
//import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Core resolution service (minimal, linear flow).
// * <p>
// * Flow:
// * 1) getBindings(assignmentId, vocabularyId, role, userId)
// * 2) loadPartySets(bindingPartySetIds)
// * 3) buildUnionSqlFor(partySets, context, q, userId)
// * 4) executePagedQuery(sql, params, limit, offset) -> List<PartyDto>, moreFlag
// * 5) build PartiesResponse and return
// * <p>
// * Keep methods small and focused so they can be extracted later.
// */
//@Service
//@RequiredArgsConstructor
//public class PartyResolverServiceCore {
//
//    private final AssignmentPartyBindingService bindingService;
//    private final PartySetRepository partySetRepo;
//    private final NamedParameterJdbcTemplate jdbc;
//    private final DSLContext dsl; // use in TODO jOOQ implementations if desired
//
//    /**
//     * Public entry: resolve parties for a given assignment/vocabulary/role and user.
//     *
//     * @param assignmentId assignment context id
//     * @param vocabularyId vocabulary (data template) id
//     * @param role         role name within template (e.g., "from","to")
//     * @param userId       current user id
//     * @param context      map of dependent field values (may be empty)
//     * @param q            optional search string
//     * @param limit        page size (client requested)
//     * @param offset       page offset
//     * @return PartiesResponse with results and provenance (partySetId)
//     */
//    @Transactional(readOnly = true)
//    public PartiesResponse resolve(String assignmentId,
//                                   String vocabularyId,
//                                   String role,
//                                   String userId,
//                                   Map<String, Object> context,
//                                   String q,
//                                   int limit,
//                                   int offset) {
//
//        // 1) bindings (may throw AccessDenied via bindingService)
//        List<AssignmentPartyBinding> bindings = getBindings(assignmentId, vocabularyId, role, userId);
//        if (bindings.isEmpty()) return emptyResponse();
//
//        // 2) load party sets referenced by bindings
//        List<UUID> psIds = bindings.stream()
//            .map(AssignmentPartyBinding::getPartySetId)
//            .filter(Objects::nonNull)
//            .distinct()
//            .collect(Collectors.toList());
//
//        List<PartySet> partySets = loadPartySets(psIds);
//        if (partySets.isEmpty()) return emptyResponse();
//
//        // 3) build union SQL and parameters
//        SqlWithParams sqlWithParams = buildUnionSqlFor(partySets, context, q, userId);
//
//        // 4) execute paged query (we fetch limit+1 to detect `moreAvailable`)
//        PagedResult<ResolvedParty> page = executePagedQuery(sqlWithParams.sql, sqlWithParams.params, limit, offset);
//
//        // 5) build response
//        PartiesResponse resp = new PartiesResponse();
//        resp.setPartySetId(psIds.stream().map(UUID::toString).collect(Collectors.joining(",")));
//        resp.setResolvedFrom("binding");
//        resp.setTotalEstimate(page.totalEstimate);
//        resp.setCacheTtlSeconds(0);
//        resp.setResults(page.items);
//        return resp;
//    }
//
//    /* -------------------------
//       Small helpers used by flow
//       ------------------------- */
//
//    /**
//     * 1) Find effective bindings for the context and user.
//     * Delegates to AssignmentPartyBindingService which enforces membership gate.
//     * Throw AccessDeniedException per Binding service semantics.
//     */
//    private List<AssignmentPartyBinding> getBindings(String assignmentId, String vocabularyId, String role, String userId) {
//        return bindingService.findEffectiveBindings(assignmentId, vocabularyId, role, userId);
//    }
//
//    /**
//     * 2) Load party_set rows. Minimal use of JPA repository.
//     */
//    private List<PartySet> loadPartySets(List<UUID> partySetIds) {
//        if (partySetIds == null || partySetIds.isEmpty()) return Collections.emptyList();
//        List<PartySet> list = partySetRepo.findAllById(partySetIds);
//        if (list == null) return Collections.emptyList();
//        return list;
//    }
//
//    /**
//     * 3) Build a single UNION SQL string and a parameter map for execution.
//     * <p>
//     * Important:
//     * - Each PartySet expands to a SELECT fragment that returns columns:
//     * id, uid, type, code, name
//     * - Permission filtering is applied here by joining user_allowed_party (server-side).
//     * - Keep the SQL plain text now; replace with jOOQ later if desired.
//     * <p>
//     * Implementation notes (TODO):
//     * - Implement expandStaticSql, expandOrgTreeSql to return a fragment and params.
//     * - Use safe param naming: ps0_root, ps0_q, etc.
//     */
//    private SqlWithParams buildUnionSqlFor(List<PartySet> partySets,
//                                           Map<String, Object> context,
//                                           String q,
//                                           String userId) {
//        StringBuilder union = new StringBuilder();
//        MapSqlParameterSource params = new MapSqlParameterSource();
//        int idx = 0;
//
//        for (PartySet ps : partySets) {
//            if (idx > 0) union.append(" UNION ALL ");
//
//            // choose expansion by kind (only STATIC and ORG_TREE are required for core)
//            PartySetKind kindValue = ps.getKind();
//            String kind = kindValue.name();
//            if ("STATIC".equalsIgnoreCase(kind)) {
//                SqlWithParams frag = expandStaticSql(ps, q, idx);
//                union.append(frag.sql);
//                mergeParams(params, frag.params);
//            } else if ("ORG_TREE".equalsIgnoreCase(kind)) {
//                SqlWithParams frag = expandOrgTreeSql(ps, context, q, idx);
//                union.append(frag.sql);
//                mergeParams(params, frag.params);
//            } else {
//                // skip other kinds for initial core
//                idx++;
//                continue;
//            }
//            idx++;
//        }
//
//        // permission clause: join user_allowed_party to enforce view
//        // we wrap union as subquery "cand" and join p and user_allowed_party
//        String finalSql = "SELECT cand.id, cand.uid, cand.type, cand.code, cand.name "
//            + "FROM ( " + union.toString() + " ) AS cand "
//            + "JOIN party p ON p.id = cand.id "
//            + "JOIN user_allowed_party uap ON uap.party_id = p.id AND uap.user_id = :__userId "
//            + "ORDER BY cand.name";
//
//        params.addValue("__userId", userId);
//        return new SqlWithParams(finalSql, params);
//    }
//
//    /**
//     * 3a) Expand STATIC party_set into SQL fragment.
//     * <p>
//     * Returns fragment like:
//     * SELECT p.id, p.uid, p.type, p.code, p.name FROM party_set_member m JOIN party p ON p.id=m.party_id
//     * WHERE m.party_set_id = :ps0_id [ AND p.name ILIKE :ps0_q ]
//     * <p>
//     * Param names are unique per idx.
//     */
//    private SqlWithParams expandStaticSql(PartySet ps, String q, int idx) {
//        String pIdParam = "__ps" + idx + "_id";
//        String qParam = "__ps" + idx + "_q";
//        StringBuilder sql = new StringBuilder();
//        sql.append("SELECT p.id, p.uid, p.type, p.code, p.name ");
//        sql.append("FROM party_set_member m JOIN party p ON p.id = m.party_id ");
//        sql.append("WHERE m.party_set_id = :").append(pIdParam);
//        MapSqlParameterSource params = new MapSqlParameterSource();
//        params.addValue(pIdParam, ps.getId());
//        if (q != null && !q.isBlank()) {
//            sql.append(" AND p.name ILIKE :").append(qParam);
//            params.addValue(qParam, "%" + q + "%");
//        }
//        return new SqlWithParams(sql.toString(), params);
//    }
//
//    /**
//     * 3b) Expand ORG_TREE party_set into SQL fragment using recursive CTE.
//     * <p>
//     * ps.spec must contain rootId and optional depth.
//     * <p>
//     * Returned SQL selects id, uid, type, code, name
//     */
//    private SqlWithParams expandOrgTreeSql(PartySet ps, Map<String, Object> context, String q, int idx) {
//        // parse root and depth from ps.getSpec() JSON (simple naive parse here; implement robustly)
//        String rootId = extractRootId(ps);
//        int depth = extractDepth(ps);
//        String rootParam = "__ps" + idx + "_root";
//        String depthParam = "__ps" + idx + "_depth";
//        String qParam = "__ps" + idx + "_q";
//
//        String sql = "WITH RECURSIVE tree AS ( "
//            + "  SELECT id, uid, type, code, name, 1 as depth FROM party WHERE id = :" + rootParam + " "
//            + "  UNION ALL "
//            + "  SELECT p.id, p.uid, p.type, p.code, p.name, tree.depth + 1 FROM party p "
//            + "  JOIN tree ON p.parent_id = tree.id "
//            + "  WHERE tree.depth < :" + depthParam + " "
//            + ") "
//            + "SELECT id, uid, type, code, name FROM tree";
//
//        MapSqlParameterSource params = new MapSqlParameterSource();
//        params.addValue(rootParam, rootId);
//        params.addValue(depthParam, depth);
//        if (q != null && !q.isBlank()) {
//            sql += " WHERE name ILIKE :" + qParam;
//            params.addValue(qParam, "%" + q + "%");
//        }
//        return new SqlWithParams(sql, params);
//    }
//
//    /**
//     * Merge params from frag into target, taking care not to clobber existing keys.
//     */
//    private void mergeParams(MapSqlParameterSource target, MapSqlParameterSource src) {
//        for (String key : src.getValues().keySet()) {
//            target.addValue(key, src.getValue(key));
//        }
//    }
//
//    /**
//     * 4) Execute SQL with limit+1 to detect more, map rows to PartyDto
//     */
//    private PagedResult<ResolvedParty> executePagedQuery(String sql, MapSqlParameterSource params, int limit, int offset) {
//        // fetch limit+1 rows
//        params.addValue("limitFetch", limit + 1);
//        params.addValue("offset", offset);
//
//        // Wrap final SQL to apply limit/offset using parameters
//        String pagedSql = "SELECT * FROM ( " + sql + " ) t LIMIT :limitFetch OFFSET :offset";
//
//        List<ResolvedParty> items = jdbc.query(pagedSql, params, (rs, rowNum) -> mapRow(rs));
//        boolean more = items.size() > limit;
//        if (more) items = items.subList(0, limit);
//        Integer totalEstimate = null; // optional: implement count if needed
//
//        return new PagedResult<>(items, more, totalEstimate);
//    }
//
//    /**
//     * Simple row mapper to PartyDto.
//     */
//    private ResolvedParty mapRow(ResultSet rs) throws SQLException {
//        return ResolvedParty.builder()
//            .id(rs.getObject("id", UUID.class))
//            .uid(rs.getString("uid"))
//            .type(rs.getString("type"))
//            .code(rs.getString("code"))
//            .name(rs.getString("name"))
//            .build();
//    }
//
//    /* -------------------------
//       Small helpers & DTOs
//       ------------------------- */
//
//    private PartiesResponse emptyResponse() {
//        PartiesResponse r = new PartiesResponse();
//        r.setPartySetId(null);
//        r.setResolvedFrom("none");
//        r.setTotalEstimate(0);
//        r.setCacheTtlSeconds(0);
//        r.setResults(Collections.emptyList());
//        return r;
//    }
//
//    private static final class SqlWithParams {
//        final String sql;
//        final MapSqlParameterSource params;
//
//        SqlWithParams(String sql, MapSqlParameterSource params) {
//            this.sql = sql;
//            this.params = params;
//        }
//    }
//
//    private static final class PagedResult<T> {
//        final List<T> items;
//        final boolean more;
//        final Integer totalEstimate;
//
//        PagedResult(List<T> items, boolean more, Integer totalEstimate) {
//            this.items = items;
//            this.more = more;
//            this.totalEstimate = totalEstimate;
//        }
//    }
//
//    /* -------------------------
//       Minimal JSON spec parsing helpers (implement robust parsing later)
//       ------------------------- */
//
//    private String extractRootId(PartySet ps) {
//        // NOTE (<=64 chars): "parse ps.spec for rootId"
//        try {
//            PartySetSpec s = ps.getSpec();
//            if (s == null) return null;
//            int p = s.indexOf("rootId");
//            if (p >= 0) {
//                int c = s.indexOf('"', p);
//                if (c >= 0) {
//                    int c2 = s.indexOf('"', c + 1);
//                    if (c2 > c) return s.substring(c + 1, c2);
//                }
//            }
//        } catch (Exception ignored) {
//        }
//        return null;
//    }
//
//    private int extractDepth(PartySet ps) {
//        // NOTE (<=64 chars): "parse ps.spec for depth"
//        try {
//            String s = ps.getSpec();
//            if (s == null) return 1000;
//            int p = s.indexOf("depth");
//            if (p >= 0) {
//                String num = s.substring(p).replaceAll("[^0-9]", "");
//                if (!num.isEmpty()) return Integer.parseInt(num);
//            }
//        } catch (Exception ignored) {
//        }
//        return 1000;
//    }
//}
