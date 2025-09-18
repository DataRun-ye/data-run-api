package org.nmcpye.datarun.analytics.projection.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.*;

/**
 * SimpleProjectionService
 * <p>
 * Starter, patched, production-light projection service used for the pilot.
 * - Loads projection_config by source_repeat_uid
 * - Batches raw_repeat_payload rows
 * - Evaluates a tiny expression language (coalesce, concat, payload->>'f', payload->'f', submission_uid, occurrence_index, template_version_uid)
 * - Writes to supply_event and breeding_source_event (supports upsert for supply, append+explode for breeding)
 * <p>
 * You must provide an OptionResolver bean that maps optionSetUid + code -> canonical uid + label JSON.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SimpleProjectionService implements ProjectionService {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleProjectionService.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper om;
    private final OptionResolver optionResolver;
    private final int batchSize = 300;

    private final Set<String> loggedPayloadKeySamples = new HashSet<>(); // class field

    private void logPayloadKeysOnce(JsonNode payload, String repeatUid) {
        // log the payload's keys once per repeatUid (avoid noisy logs)
        if (payload == null) return;
        if (loggedPayloadKeySamples.contains(repeatUid)) return;
        List<String> keys = new ArrayList<>();
        payload.fieldNames().forEachRemaining(keys::add);
        LOG.info("Sample payload keys for repeatUid {}: {}", repeatUid, keys);
        loggedPayloadKeySamples.add(repeatUid);
    }

    @Transactional
    @Override
    public void runForRepeat(String repeatUid) throws Exception {
        JsonNode config = loadConfigForRepeat(repeatUid);
        if (config == null) throw new IllegalStateException("no projection_config for repeat " + repeatUid);

        JsonNode target = config.at("/target");
        String table = target.get("table").asText();

        int offset = 0;
        while (true) {
            List<Map<String, Object>> rows = fetchBatch(repeatUid, batchSize, offset);
            if (rows.isEmpty()) break;

            if ("supply_event".equals(table)) {
                processSupplyBatch(config, rows, repeatUid);
            } else if ("breeding_source_event".equals(table)) {
                processBreedingBatch(config, rows, repeatUid);
            } else {
                throw new IllegalStateException("unsupported target table: " + table);
            }

            offset += rows.size();
        }
    }

    /* ---------- config + fetch ---------- */
    private JsonNode loadConfigForRepeat(String repeatUid) {
        String sql = "SELECT payload FROM projection_config WHERE source_repeat_uid = :r LIMIT 1";
        List<JsonNode> configs = jdbc.query(sql, Collections.singletonMap("r", repeatUid), (rs, rowNum) -> {
            try {
                return om.readTree(rs.getString("payload"));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        return configs.isEmpty() ? null : configs.get(0);
    }

    private List<Map<String, Object>> fetchBatch(String repeatUid, int limit, int offset) {
        String sql = "SELECT id, submission_uid, payload, occurrence_index, template_version_uid FROM raw_repeat_payload WHERE repeat_uid = :r ORDER BY created_at, id LIMIT :l OFFSET :o";
        Map<String, Object> params = Map.of("r", repeatUid, "l", limit, "o", offset);
        return jdbc.query(sql, params, (rs, rowNum) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", rs.getObject("id"));
            m.put("submission_uid", rs.getString("submission_uid"));
            try {
                String payloadStr = rs.getString("payload");
                m.put("payload", payloadStr != null ? om.readTree(payloadStr) : null);
            } catch (Exception ex) {
                m.put("payload", null);
            }
            m.put("occurrence_index", rs.getInt("occurrence_index"));
            m.put("template_version_uid", rs.getString("template_version_uid"));
            return m;
        });
    }

    /* ---------- Supply (upsert) ---------- */
    @Transactional
    protected void processSupplyBatch(JsonNode config, List<Map<String, Object>> rows, String repeatUid) throws Exception {
        JsonNode target = config.at("/target");
        JsonNode mappings = target.get("mappings");
        String naturalKeyExpr = target.get("naturalKeyExpr").asText();

        String insertSql = "INSERT INTO supply_event (id, natural_key, submission_uid, template_uid, template_version_uid, occurrence_index, payload_id, amd_uid, amd_label, month_name_uid, supply_date, stockout_days, expired_quantity, consumed_quantity, received_quantity, available_quantity, redistributed_quantity, extra, created_at, updated_at) " +
            "VALUES (:id, :natural_key, :submission_uid, :template_uid, :template_version_uid, :occurrence_index, :payload_id, :amd_uid, :amd_label::jsonb, :month_name_uid, :supply_date, :stockout_days, :expired_quantity, :consumed_quantity, :received_quantity, :available_quantity, :redistributed_quantity, :extra::jsonb, now(), now()) " +
            "ON CONFLICT (natural_key) DO UPDATE SET amd_uid = EXCLUDED.amd_uid, amd_label = EXCLUDED.amd_label, month_name_uid = EXCLUDED.month_name_uid, supply_date = EXCLUDED.supply_date, stockout_days = EXCLUDED.stockout_days, expired_quantity = EXCLUDED.expired_quantity, consumed_quantity = EXCLUDED.consumed_quantity, received_quantity = EXCLUDED.received_quantity, available_quantity = EXCLUDED.available_quantity, redistributed_quantity = EXCLUDED.redistributed_quantity, extra = EXCLUDED.extra, updated_at = now()";

        List<Map<String, Object>> batchParams = new ArrayList<>();
        boolean printedSample = false;

        for (Map<String, Object> r : rows) {
            JsonNode payload = (JsonNode) r.get("payload");
            logPayloadKeysOnce(payload, repeatUid);
            String submissionUid = (String) r.get("submission_uid");
            int occIndex = (Integer) r.get("occurrence_index");
            String templateVersionUid = (String) r.get("template_version_uid");

            String naturalKey = evalExprString(naturalKeyExpr, payload, submissionUid, occIndex, templateVersionUid);

            Map<String, Object> params = new HashMap<>();
            params.put("id", UUID.randomUUID());
            params.put("natural_key", naturalKey);
            params.put("submission_uid", submissionUid);
            params.put("template_uid", null);
            params.put("template_version_uid", templateVersionUid);
            params.put("occurrence_index", occIndex);
            params.put("payload_id", safeEvalAsString("payload->_id", payload));

            // amd: resolve option and set uid and labelJson (or null)
            String amdCode = safeEvalAsString("payload->>'amd'", payload);
            String amdUid = null;
            String amdLabelJson = null;
            if (amdCode != null) {
                OptionResolution or = optionResolver.resolve(optionKeyFromConfig(mappings, "amd_uid"), amdCode);
                if (or != null) {
                    amdUid = or.uid;
                    amdLabelJson = or.labelJson;
                }
            }
            params.put("amd_uid", amdUid);
            params.put("amd_label", amdLabelJson); // allow null

            params.put("month_name_uid", safeEvalAsString("payload->>'month_name'", payload));

            // >>> IMPORTANT: convert supply_date to java.sql.Date (or null)
            java.sql.Date supplyDate = parseDate(safeEvalAsString("payload->>'supply_date'", payload));
            params.put("supply_date", supplyDate);

            params.put("stockout_days", toInteger(safeEvalAsString("payload->>'stockout_days'", payload)));
            params.put("expired_quantity", toInteger(safeEvalAsString("payload->>'expired_quantity'", payload)));
            params.put("consumed_quantity", toInteger(safeEvalAsString("payload->>'consumed_quantity'", payload)));
            params.put("received_quantity", toInteger(safeEvalAsString("payload->>'received_quantity'", payload)));
            params.put("available_quantity", toInteger(safeEvalAsString("payload->>'available_quantity'", payload)));
            params.put("redistributed_quantity", toInteger(safeEvalAsString("payload->>'redistributed_quantity'", payload)));
            params.put("extra", payload != null ? payload.toString() : null);

            batchParams.add(params);

            if (!printedSample) {
                printedSample = true;
                LOG.info("Sample mapping values (first row) for repeat={}, naturalKey={}: {}", repeatUid, naturalKey, params);
            }

            if (batchParams.size() >= batchSize) {
                try {
                    jdbc.batchUpdate(insertSql, SqlParameterSourceUtils.createBatch(batchParams.toArray()));
                } catch (Exception ex) {
                    LOG.error("Batch insert error for supply_event. First sample params: {}", batchParams.get(0), ex);
                    throw ex;
                }
                batchParams.clear();
            }
        }
        if (!batchParams.isEmpty()) {
            try {
                jdbc.batchUpdate(insertSql, SqlParameterSourceUtils.createBatch(batchParams.toArray()));
            } catch (Exception ex) {
                LOG.error("Final batch insert error for supply_event. First sample params: {}", batchParams.get(0), ex);
                throw ex;
            }
        }
    }

    /* ---------- Breeding (append + explode) ---------- */
    @Transactional
    protected void processBreedingBatch(JsonNode config, List<Map<String, Object>> rows, String repeatUid) throws Exception {
        JsonNode target = config.at("/target");
        JsonNode mappings = target.get("mappings");

        String insertSql = "INSERT INTO breeding_source_event (id, natural_key, submission_uid, template_uid, template_version_uid, projection_run_id, occurrence_index, household_name, breeding_habitat_type_uid, breeding_habitat_type_label, larval_stage_presence, growth_regulator_grams, breeding_habitat_description, breeding_habitats_count, breeding_habitat_length_uid, breeding_habitat_width_uid, breeding_habitat_depth_uid, temphos_ml, lsm_types, extra, payload_checksum, created_at) " +
            "VALUES (:id, :natural_key, :submission_uid, :template_uid, :template_version_uid, gen_random_uuid(), :occurrence_index, :household_name, :breeding_habitat_type_uid, :breeding_habitat_type_label::jsonb, :larval_stage_presence, :growth_regulator_grams, :breeding_habitat_description, :breeding_habitats_count, :breeding_habitat_length_uid, :breeding_habitat_width_uid, :breeding_habitat_depth_uid, :temphos_ml, :lsm_types::jsonb, :extra::jsonb, :payload_checksum, now())";

        String insertLsmSql = "INSERT INTO breeding_lsm_type (id, breeding_event_id, lsm_type_uid, lsm_type_label) VALUES (:id, :breedingEventId, :lsmTypeUid, :lsmTypeLabel::jsonb)";

        List<Map<String, Object>> mainBatch = new ArrayList<>();
        List<Map<String, Object>> lsmBatch = new ArrayList<>();
        boolean printedSample = false;

        for (Map<String, Object> r : rows) {
            JsonNode payload = (JsonNode) r.get("payload");
            logPayloadKeysOnce(payload, repeatUid);
            String submissionUid = (String) r.get("submission_uid");
            int occIndex = (Integer) r.get("occurrence_index");
            String templateVersionUid = (String) r.get("template_version_uid");

            String naturalKey = evalExprString(target.get("naturalKeyExpr").asText(), payload, submissionUid, occIndex, templateVersionUid);

            UUID id = UUID.randomUUID();
            Map<String, Object> params = new HashMap<>();
            params.put("id", id);
            params.put("natural_key", naturalKey);
            params.put("submission_uid", submissionUid);
            params.put("template_uid", null);
            params.put("template_version_uid", templateVersionUid);
            params.put("occurrence_index", occIndex);
            params.put("household_name", safeEvalAsString("payload->>'householdname'", payload));
            String habitatType = safeEvalAsString("payload->>'breeding_habitat_type'", payload);
            params.put("breeding_habitat_type_uid", habitatType);
            OptionResolution or = optionResolver.resolve(optionKeyFromConfig(mappings, "breeding_habitat_type_uid"), habitatType);
            params.put("breeding_habitat_type_label", or != null ? or.labelJson : null);
            params.put("larval_stage_presence", toBoolean(safeEvalAsString("payload->>'Larval_stage_presence'", payload)));
            params.put("growth_regulator_grams", toInteger(safeEvalAsString("payload->>'growth_regulator_grams'", payload)));
            params.put("breeding_habitat_description", safeEvalAsString("payload->>'breeding_habitat_description'", payload));
            params.put("breeding_habitats_count", toInteger(safeEvalAsString("payload->>'breeding_habitats_count'", payload)));
            params.put("breeding_habitat_length_uid", safeEvalAsString("payload->>'breeding_habitat_length_meter'", payload));
            params.put("breeding_habitat_width_uid", safeEvalAsString("payload->>'breeding_habitat_width_meter'", payload));
            params.put("breeding_habitat_depth_uid", safeEvalAsString("payload->>'breeding_habitat_depth_meter'", payload));
            params.put("temphos_ml", toBigDecimal(safeEvalAsString("payload->>'temphos_ml'", payload)));
            JsonNode lsmNode = payload != null ? payload.get("lsm_type") : null;
            params.put("lsm_types", lsmNode != null ? lsmNode.toString() : null);
            params.put("extra", payload != null ? payload.toString() : null);
            params.put("payload_checksum", computePayloadChecksum(payload, "breeding_habitat_type", "householdname", "growth_regulator_grams"));

            mainBatch.add(params);

            if (lsmNode != null && lsmNode.isArray()) {
                for (JsonNode option : (ArrayNode) lsmNode) {
                    String lsmUid = option.isTextual() ? option.asText() : option.toString();
                    OptionResolution lsmOr = optionResolver.resolve(optionKeyFromConfig(mappings, "lsm_types"), lsmUid);
                    Map<String, Object> p = new HashMap<>();
                    p.put("id", UUID.randomUUID());
                    p.put("breedingEventId", id);
                    p.put("lsmTypeUid", lsmUid);
                    p.put("lsmTypeLabel", lsmOr != null ? lsmOr.labelJson : null);
                    lsmBatch.add(p);
                }
            }

            if (!printedSample) {
                printedSample = true;
                LOG.info("Sample mapping values (first row) for repeat={}, naturalKey={}: {}", repeatUid, naturalKey, params);
            }

            if (mainBatch.size() >= batchSize) {
                jdbc.batchUpdate(insertSql, SqlParameterSourceUtils.createBatch(mainBatch.toArray()));
                mainBatch.clear();
            }
            if (lsmBatch.size() >= batchSize) {
                jdbc.batchUpdate(insertLsmSql, SqlParameterSourceUtils.createBatch(lsmBatch.toArray()));
                lsmBatch.clear();
            }
        }

        if (!mainBatch.isEmpty()) jdbc.batchUpdate(insertSql, SqlParameterSourceUtils.createBatch(mainBatch.toArray()));
        if (!lsmBatch.isEmpty())
            jdbc.batchUpdate(insertLsmSql, SqlParameterSourceUtils.createBatch(lsmBatch.toArray()));
    }

    /* ---------- tiny expression evaluator (limited) ---------- */

    private String evalExprString(String expr, JsonNode payload, String submissionUid, int occurrenceIndex, String templateVersionUid) {
        expr = expr.trim();
        if (expr.startsWith("coalesce(") && expr.endsWith(")")) {
            String inner = expr.substring(9, expr.length() - 1);
            String[] parts = splitTopLevelComma(inner);
            for (String p : parts) {
                String v = evalExprString(p.trim(), payload, submissionUid, occurrenceIndex, templateVersionUid);
                if (v != null && !v.isEmpty()) return v;
            }
            return null;
        }
        if (expr.startsWith("concat(") && expr.endsWith(")")) {
            String inner = expr.substring(7, expr.length() - 1);
            String[] parts = splitTopLevelComma(inner);
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                String v = evalExprString(p.trim(), payload, submissionUid, occurrenceIndex, templateVersionUid);
                if (v != null) sb.append(v);
            }
            return sb.toString();
        }
        if ("submission_uid".equals(expr)) return submissionUid;
        if ("occurrence_index".equals(expr)) return String.valueOf(occurrenceIndex);
        if ("template_version_uid".equals(expr)) return templateVersionUid;

        return safeEvalAsString(expr, payload);
    }

    private String[] splitTopLevelComma(String s) {
        return s.split("\\s*,\\s*");
    }

    private String safeEvalAsString(String token, JsonNode payload) {
        if (token == null) return null;
        token = token.trim();
        if (token.equals("payload->_id")) {
            JsonNode n = payload != null ? payload.get("_id") : null;
            return n != null && !n.isNull() ? n.asText() : null;
        }
        if (token.startsWith("payload->>'") && token.endsWith("'")) {
            String f = token.substring(11, token.length() - 1);
            JsonNode n = payload != null ? payload.get(f) : null;
            return n != null && !n.isNull() ? n.asText() : null;
        }
        if (token.startsWith("payload->'") && token.endsWith("'")) {
            String f = token.substring(9, token.length() - 1);
            JsonNode n = payload != null ? payload.get(f) : null;
            return n != null && !n.isNull() ? n.toString() : null;
        }
        if ((token.startsWith("'") && token.endsWith("'")) || (token.startsWith("\"") && token.endsWith("\""))) {
            return token.substring(1, token.length() - 1);
        }
        return token;
    }

    private java.sql.Date parseDate(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            // Try ISO yyyy-MM-dd first (most common in your payloads)
            java.time.LocalDate ld = java.time.LocalDate.parse(s);
            return java.sql.Date.valueOf(ld);
        } catch (Exception e1) {
            try {
                // Try common other timestamp formats, e.g. '2025-09-14 00:00' or '2025-09-14T00:00:00.000'
                String dateOnly = s.split("T")[0].split(" ")[0];
                java.time.LocalDate ld2 = java.time.LocalDate.parse(dateOnly);
                return java.sql.Date.valueOf(ld2);
            } catch (Exception e2) {
                LOG.warn("parseDate: cannot parse date value '{}', returning null", s);
                return null;
            }
        }
    }

    private Integer toInteger(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return new BigDecimal(s);
        } catch (Exception ex) {
            return null;
        }
    }

    private Boolean toBoolean(String s) {
        if (s == null) return null;
        s = s.trim().toLowerCase();
        if (s.equals("true") || s.equals("t") || s.equals("1")) return Boolean.TRUE;
        if (s.equals("false") || s.equals("f") || s.equals("0")) return Boolean.FALSE;
        return null;
    }

    private String optionKeyFromConfig(JsonNode mappings, String mappingKey) {
        JsonNode node = mappings.get(mappingKey);
        if (node != null && node.has("optionSetUid")) return node.get("optionSetUid").asText();
        return null;
    }

    private String computePayloadChecksum(JsonNode payload, String... fields) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (String f : fields) {
                JsonNode n = payload != null ? payload.get(f) : null;
                sb.append(n != null && !n.isNull() ? n.asText() : "");
                sb.append("|");
            }
            byte[] dig = md.digest(sb.toString().getBytes("UTF-8"));
            return bytesToHex(dig);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
