//package org.nmcpye.datarun.analytics.projection;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//import java.time.OffsetDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
///**
// * @author Hamza Assada
// * @since 17/09/2025
// */
//@Component
//public class ProjectionService {
//    private final NamedParameterJdbcTemplate jdbc;
//    private final ObjectMapper objectMapper;
//
//    public ProjectionService(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
//        this.jdbc = jdbc;
//        this.objectMapper = objectMapper;
//    }
//
//    public List<RawRepeat> fetchRawRepeatRows(String repeatPath, int limit) {
//        String sql = "SELECT id, submission_uid, payload, occurrence_index FROM raw_repeat_payload WHERE repeat_path = :path LIMIT :limit";
//        Map<String,Object> p = Map.of("path", repeatPath, "limit", limit);
//        return jdbc.query(sql, p, (rs, rowNum) -> {
//            RawRepeat r = new RawRepeat();
//            r.setId(UUID.fromString(rs.getString("id")));
//            r.setSubmissionUid(rs.getString("submission_uid"));
//            r.setOccurrenceIndex(rs.getInt("occurrence_index"));
//            r.setPayload(rs.getString("payload"));
//            return r;
//        });
//    }
//
//    public ProjectionConfig loadProjectionConfig(String uid) throws Exception {
//        String sql = "SELECT payload::text FROM projection_config WHERE uid = :uid";
//        Map<String,Object> p = Map.of("uid", uid);
//        String payloadText = jdbc.queryForObject(sql, p, String.class);
//        JsonNode root = objectMapper.readTree(payloadText);
//        return ProjectionConfig.fromJson(root);
//    }
//
//    @Transactional
//    public void projectOrderLinesForRepeat(String projectionConfigUid) throws Exception {
//        ProjectionConfig cfg = loadProjectionConfig(projectionConfigUid);
//        String repeatPath = cfg.getSourceRepeatPath();
//        List<RawRepeat> rows = fetchRawRepeatRows(repeatPath, 1000);
//        UUID runId = UUID.randomUUID();
//
//        for (RawRepeat r : rows) {
//            JsonNode payload = objectMapper.readTree(r.getPayload());
//            // compute natural key deterministically. Here we follow the configured expression
//            String orderUid = safeText(payload, "order_uid");
//            String lineNumber = safeText(payload, "line_number");
//            String naturalKey = r.getSubmissionUid() + "|" + orderUid + "|" + lineNumber;
//            String naturalHash = sha256Hex(naturalKey);
//
//            // map fields
//            String productUid = safeText(payload, "product_uid");
//            BigDecimal quantity = safeDecimal(payload, "quantity");
//            BigDecimal price = safeDecimal(payload, "price");
//            OffsetDateTime eventTs = parseTimestamp(payload, "timestamp");
//
//            Map<String,Object> params = new HashMap<>();
//            params.put("id", UUID.randomUUID());
//            params.put("submissionUid", r.getSubmissionUid());
//            params.put("runId", runId);
//            params.put("naturalKey", naturalHash);
//            params.put("orderUid", orderUid);
//            params.put("productUid", productUid);
//            params.put("quantity", quantity);
//            params.put("price", price);
//            params.put("eventTs", eventTs);
//            params.put("extra", payload.toString());
//
//            String upsertSql = loadUpsertSql(); // you can keep SQL in a property or resource file
//            jdbc.update(upsertSql, params);
//        }
//    }
//
//    private static String safeText(JsonNode n, String key) {
//        JsonNode v = n.get(key);
//        return (v == null || v.isNull()) ? null : v.asText();
//    }
//
//    private static BigDecimal safeDecimal(JsonNode n, String key) {
//        JsonNode v = n.get(key);
//        if (v == null || v.isNull()) return null;
//        try { return new BigDecimal(v.asText()); } catch(Exception ex) { return null; }
//    }
//
//    private static OffsetDateTime parseTimestamp(JsonNode n, String key) {
//        JsonNode v = n.get(key);
//        if (v == null || v.isNull()) return null;
//        return OffsetDateTime.parse(v.asText());
//    }
//
//    private static String sha256Hex(String input) throws Exception {
//        MessageDigest md = MessageDigest.getInstance("SHA-256");
//        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
//        StringBuilder sb = new StringBuilder();
//        for (byte b : digest) sb.append(String.format("%02x", b));
//        return sb.toString();
//    }
//
//    // RawRepeat and ProjectionConfig are simple POJOs (omitted for brevity) - create them with fields used above.
//}
