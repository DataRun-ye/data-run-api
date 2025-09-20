//package org.nmcpye.datarun.analytics.projection;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.NullNode;
//import org.springframework.stereotype.Component;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.time.format.DateTimeParseException;
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * Lightweight, safe expression evaluator used by the projection runner.
// * <p>
// * Supported tokens / forms (minimal, whitelist-based):
// * - payload->_id
// * - payload->>'fieldName'
// * - payload->'fieldName'  (returns JsonNode)
// * - submission_uid, occurrence_index (provided via context map)
// * - function calls: TO_INT(...), TO_NUM(...), TO_BOOL(...), PARSE_DATE(...), CONCAT(...)
// * - option helpers: OPTION_RESOLVE(codeExpr, optionSetUid), OPTION_LABEL(codeExpr, optionSetUid)
// * <p>
// * The evaluator intentionally avoids arbitrary script execution. It performs a small syntactic parse
// * and evaluates functions recursively. It is not a full expression language, but is extendable.
// */
//@Service
//public class ExpressionEvaluator {
//
//    private final OptionService optionService;
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    // Recognize function calls like NAME(arg1,arg2)
//    private static final Pattern FUNC_PATTERN = Pattern.compile("^([A-Z_][A-Z0-9_]*)\\((.*)\\)$",
//        Pattern.CASE_INSENSITIVE);
//
//    // payload->>'field'
//    private static final Pattern PAYLOAD_TEXT_PATTERN = Pattern.compile("^payload->>'([a-zA-Z0-9_]+)'$");
//    // payload->'field'
//    private static final Pattern PAYLOAD_NODE_PATTERN = Pattern.compile("^payload->'([a-zA-Z0-9_]+)'$");
//    // payload->_id
//    private static final Pattern PAYLOAD_ID_PATTERN = Pattern.compile("^payload->_id$");
//
//    public ExpressionEvaluator(OptionService optionService) {
//        this.optionService = optionService;
//    }
//
//    // Public typed helpers -------------------------------------------------
//
//    public String evalString(String expr, JsonNode payload, Map<String, Object> ctx) {
//        Object v = eval(expr, payload, ctx);
//        return v == null ? null : v.toString();
//    }
//
//    public Integer evalInt(String expr, JsonNode payload, Map<String, Object> ctx) {
//        Object v = eval(expr, payload, ctx);
//        if (v == null) return null;
//        if (v instanceof Integer) return (Integer) v;
//        if (v instanceof Number) return ((Number) v).intValue();
//        try {
//            return Integer.parseInt(v.toString());
//        } catch (NumberFormatException e) {
//            return null;
//        }
//    }
//
//    public BigDecimal evalNum(String expr, JsonNode payload, Map<String, Object> ctx) {
//        Object v = eval(expr, payload, ctx);
//        if (v == null) return null;
//        try {
//            return new BigDecimal(v.toString());
//        } catch (NumberFormatException e) {
//            return null;
//        }
//    }
//
//    public Boolean evalBool(String expr, JsonNode payload, Map<String, Object> ctx) {
//        Object v = eval(expr, payload, ctx);
//        if (v == null) return null;
//        if (v instanceof Boolean) return (Boolean) v;
//        String s = v.toString().trim().toLowerCase(Locale.ROOT);
//        if (s.equals("true") || s.equals("t") || s.equals("1") || s.equals("yes") || s.equals("y")) return true;
//        if (s.equals("false") || s.equals("f") || s.equals("0") || s.equals("no") || s.equals("n")) return false;
//        return null;
//    }
//
//    public LocalDate evalDate(String expr, JsonNode payload, Map<String, Object> ctx) {
//        Object v = eval(expr, payload, ctx);
//        if (v == null) return null;
//        String s = v.toString().trim();
//        if (s.isEmpty()) return null;
//        List<DateTimeFormatter> fmts = Arrays.asList(
//            DateTimeFormatter.ISO_LOCAL_DATE,
//            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
//            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
//            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
//            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
//        );
//        for (DateTimeFormatter fmt : fmts) {
//            try {
//                return LocalDate.parse(s, fmt);
//            } catch (DateTimeParseException ignored) {
//            }
//        }
//        // Last attempt: parse only the date prefix
//        try {
//            return LocalDate.parse(s.substring(0, 10));
//        } catch (Exception ex) {
//            return null;
//        }
//    }
//
//    // Core evaluation - recursive -----------------------------------------
//    public Object eval(String expr, JsonNode payload, Map<String, Object> ctx) {
//        if (expr == null) return null;
//        expr = expr.trim();
//        if (expr.isEmpty()) return null;
//
//        // literal string quoted with single quotes
//        if (expr.startsWith("'") && expr.endsWith("'") && expr.length() >= 2) {
//            return expr.substring(1, expr.length() - 1);
//        }
//
//        // direct context fields
//        if (ctx != null && ctx.containsKey(expr)) {
//            return ctx.get(expr);
//        }
//
//        // payload->_id
//        if (PAYLOAD_ID_PATTERN.matcher(expr).matches()) {
//            JsonNode n = payload == null ? NullNode.getInstance() : payload.get("_id");
//            return n == null || n.isNull() ? null : n.asText();
//        }
//
//        // payload->>'field'
//        Matcher mText = PAYLOAD_TEXT_PATTERN.matcher(expr);
//        if (mText.matches()) {
//            String f = mText.group(1);
//            JsonNode n = payload == null ? NullNode.getInstance() : payload.get(f);
//            return n == null || n.isNull() ? null : n.asText();
//        }
//
//        // payload->'field' (JsonNode)
//        Matcher mNode = PAYLOAD_NODE_PATTERN.matcher(expr);
//        if (mNode.matches()) {
//            String f = mNode.group(1);
//            JsonNode n = payload == null ? NullNode.getInstance() : payload.get(f);
//            return n == null || n.isNull() ? null : n;
//        }
//
//        // numeric literal
//        if (expr.matches("^-?\\d+$")) {
//            return Integer.parseInt(expr);
//        }
//        if (expr.matches("^-?\\d+\\.\\d+$")) {
//            return new BigDecimal(expr);
//        }
//
//        // function call
//        Matcher func = FUNC_PATTERN.matcher(expr);
//        if (func.matches()) {
//            String fn = func.group(1).toUpperCase(Locale.ROOT);
//            String argsRaw = func.group(2).trim();
//            List<String> args = splitArgs(argsRaw);
//            return evalFunction(fn, args, payload, ctx);
//        }
//
//        // fallback: maybe it's a bare identifier or JSONPath, try payload direct
//        if (payload != null && payload.has(expr)) {
//            JsonNode n = payload.get(expr);
//            return n == null || n.isNull() ? null : n.asText();
//        }
//
//        // nothing matched
//        return null;
//    }
//
//    // Split top-level args by comma, respect nested parentheses and single quotes
//    private List<String> splitArgs(String src) {
//        List<String> out = new ArrayList<>();
//        if (src == null || src.isEmpty()) return out;
//        int depth = 0;
//        boolean inQuotes = false;
//        StringBuilder cur = new StringBuilder();
//        for (int i = 0; i < src.length(); i++) {
//            char c = src.charAt(i);
//            if (c == '\'') {
//                // toggle quotes
//                inQuotes = !inQuotes;
//                cur.append(c);
//                continue;
//            }
//            if (!inQuotes) {
//                if (c == '(') {
//                    depth++;
//                    cur.append(c);
//                    continue;
//                }
//                if (c == ')') {
//                    depth--;
//                    cur.append(c);
//                    continue;
//                }
//                if (c == ',' && depth == 0) {
//                    out.add(cur.toString().trim());
//                    cur.setLength(0);
//                    continue;
//                }
//            }
//            cur.append(c);
//        }
//        if (cur.length() > 0) out.add(cur.toString().trim());
//        return out;
//    }
//
//    private Object evalFunction(String fn, List<String> args, JsonNode payload, Map<String, Object> ctx) {
//        switch (fn) {
//            case "TO_INT":
//                if (args.size() != 1) return null;
//                Object v = eval(args.get(0), payload, ctx);
//                if (v == null) return null;
//                try {
//                    return Integer.parseInt(v.toString());
//                } catch (NumberFormatException e) {
//                    return null;
//                }
//            case "TO_NUM":
//                if (args.size() != 1) return null;
//                Object v2 = eval(args.get(0), payload, ctx);
//                if (v2 == null) return null;
//                try {
//                    return new BigDecimal(v2.toString());
//                } catch (Exception e) {
//                    return null;
//                }
//            case "TO_BOOL":
//                if (args.size() != 1) return null;
//                Object v3 = eval(args.get(0), payload, ctx);
//                if (v3 == null) return null;
//                String s = v3.toString().trim().toLowerCase(Locale.ROOT);
//                if (s.equals("true") || s.equals("t") || s.equals("1")) return true;
//                if (s.equals("false") || s.equals("f") || s.equals("0")) return false;
//                return null;
//            case "PARSE_DATE":
//                if (args.size() != 1) return null;
//                Object v4 = eval(args.get(0), payload, ctx);
//                if (v4 == null) return null;
//                try {
//                    return evalDate(args.get(0), payload, ctx);
//                } catch (Exception e) {
//                    return null;
//                }
//            case "CONCAT":
//                StringBuilder sb = new StringBuilder();
//                for (String a : args) {
//                    Object vv = eval(a, payload, ctx);
//                    if (vv != null) sb.append(vv.toString());
//                }
//                return sb.toString();
//            case "OPTION_RESOLVE":
//                if (args.size() != 2) return null;
//                Object codeObj = eval(args.get(0), payload, ctx);
//                String code = codeObj == null ? null : codeObj.toString();
//                String optionSetUid = trimQuotes(args.get(1));
//                if (code == null) return null;
//                return optionService.resolveOptionUid(optionSetUid, code).orElse(null);
//            case "OPTION_LABEL":
//                if (args.size() != 2) return null;
//                Object codeObj2 = eval(args.get(0), payload, ctx);
//                String code2 = codeObj2 == null ? null : codeObj2.toString();
//                String optionSetUid2 = trimQuotes(args.get(1));
//                if (code2 == null) return null;
//                return optionService.resolveOptionLabel(optionSetUid2, code2);
//            default:
//                return null; // unsupported function -> null
//        }
//    }
//
//    private String trimQuotes(String s) {
//        if (s == null) return null;
//        s = s.trim();
//        if ((s.startsWith("'") && s.endsWith("'") && s.length() >= 2) || (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2)) {
//            return s.substring(1, s.length() - 1);
//        }
//        return s;
//    }
//}
//
//
//// Option service contract and a tiny in-memory example implementation
//interface OptionService {
//    Optional<String> resolveOptionUid(String optionSetUid, String code);
//
//    JsonNode resolveOptionLabel(String optionSetUid, String code);
//}
//
//@Component
//class InMemoryOptionService implements OptionService {
//    private final Map<String, Map<String, OptionEntry>> store = new HashMap<>();
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    public InMemoryOptionService() {
//        // sample seed - in production replace with a repository-backed implementation
//        Map<String, OptionEntry> m = new HashMap<>();
//        m.put("mrdt_cassette", new OptionEntry("nldwH9Uc3kW", mapper.createObjectNode().put("en", "mrdt_cassette").put("ar", "الفحص السريع للملاريا - بالشريط")));
//        store.put("sYiS5D2qeG8", m);
//    }
//
//    @Override
//    public Optional<String> resolveOptionUid(String optionSetUid, String code) {
//        if (optionSetUid == null || code == null) return Optional.empty();
//        Map<String, OptionEntry> m = store.get(optionSetUid);
//        if (m == null) return Optional.empty();
//        OptionEntry oe = m.get(code);
//        return oe == null ? Optional.empty() : Optional.of(oe.uid);
//    }
//
//    @Override
//    public JsonNode resolveOptionLabel(String optionSetUid, String code) {
//        if (optionSetUid == null || code == null) return NullNode.getInstance();
//        Map<String, OptionEntry> m = store.get(optionSetUid);
//        if (m == null) return NullNode.getInstance();
//        OptionEntry oe = m.get(code);
//        return oe == null ? NullNode.getInstance() : oe.label;
//    }
//
//    static class OptionEntry {
//        final String uid;
//        final JsonNode label;
//
//        OptionEntry(String uid, JsonNode label) {
//            this.uid = uid;
//            this.label = label;
//        }
//    }
//}
//
