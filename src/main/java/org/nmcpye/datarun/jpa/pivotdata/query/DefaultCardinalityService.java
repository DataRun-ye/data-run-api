//package org.nmcpye.datarun.jpa.pivotdata.query;
//
//import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Instant;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * Simple estimator + in-memory TTL cache.
// * <p>
// * NOTE: This is intentionally conservative and simple for Phase0.
// * Replace with a more robust approach when you have production telemetry / stats.
// *
// * @author Hamza Assada 19/08/2025 (7amza.it@gmail.com)
// */
//@Service
//public class DefaultCardinalityService implements CardinalityService {
//
//    private final NamedParameterJdbcTemplate jdbc;
//    // simple cache: key -> (value, expiry)
//    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
//    private final long ttlMs;
//
//    public DefaultCardinalityService(NamedParameterJdbcTemplate jdbc, long ttlMs) {
//        this.jdbc = jdbc;
//        this.ttlMs = ttlMs;
//    }
//
//    @Override
//    public Long getCardinalityHint(String dimensionId, String templateId) {
//        String key = dimensionId + "|" + (templateId == null ? "" : templateId);
//        CacheEntry entry = cache.get(key);
//        if (entry != null && entry.expiry > Instant.now().toEpochMilli()) return entry.value;
//
//        // Attempt a fast sampled estimate (1%).
//        try {
//            String sampleSql = String.format(
//                "SELECT COUNT(DISTINCT %s) as dcount " +
//                    "FROM (SELECT %s FROM element_data_value TABLESAMPLE SYSTEM (1) WHERE template_id = :tid AND deleted_at IS NULL) s",
//                quoted(dimensionId), quoted(dimensionId));
//            MapSqlParameterSource params = new MapSqlParameterSource().addValue("tid", templateId);
//            Long sample = jdbc.queryForObject(sampleSql, params, Long.class);
//            if (sample != null && sample > 0) {
//                // scale roughly by 100 (since sample 1%)
//                long est = Math.min(10_000_000L, sample * 100L);
//                cache.put(key, new CacheEntry(est, Instant.now().toEpochMilli() + ttlMs));
//                return est;
//            }
//        } catch (Exception ex) {
//            // sampling may fail on some installations (TABLESAMPLE not supported or dimension expression not directly selectable)
//        }
//
//        // fallback: try a direct distinct count but with LIMIT fallback to avoid long running query
//        try {
//            String exactSql = String.format("SELECT COUNT(DISTINCT %s) FROM element_data_value WHERE template_id = :tid AND deleted_at IS NULL", quoted(dimensionId));
//            MapSqlParameterSource params = new MapSqlParameterSource().addValue("tid", templateId);
//            Long exact = jdbc.queryForObject(exactSql, params, Long.class);
//            cache.put(key, new CacheEntry(exact == null ? 0L : exact, Instant.now().toEpochMilli() + ttlMs));
//            return exact;
//        } catch (Exception ex) {
//            // give up gracefully
//            cache.put(key, new CacheEntry(1000L, Instant.now().toEpochMilli() + ttlMs)); // conservative default
//            return 1000L;
//        }
//    }
//
//    private static class CacheEntry {
//        final Long value;
//        final long expiry;
//
//        CacheEntry(Long value, long expiry) {
//            this.value = value;
//            this.expiry = expiry;
//        }
//    }
//
//    private String quoted(String expr) {
//        // naive: if it's a plain identifier, wrap in double quotes; otherwise assume user provided safe expression.
//        if (expr.matches("[a-zA-Z0-9_.]+")) return "\"" + expr + "\"";
//        return expr;
//    }
//}
