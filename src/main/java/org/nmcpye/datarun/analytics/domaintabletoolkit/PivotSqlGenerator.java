//package org.nmcpye.datarun.analytics.domaintabletoolkit;
//
//import org.nmcpye.datarun.analytics.domaintabletoolkit.model.CeMeta;
//import org.nmcpye.datarun.analytics.domaintabletoolkit.model.PivotSpec;
//
//import java.security.MessageDigest;
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//public class PivotSqlGenerator {
//
//    private static final int MAX_CE = 500; // safety guard
//
//    public static String pivotHash(PivotSpec spec) {
//        try {
//            MessageDigest md = MessageDigest.getInstance("SHA-256");
//            md.update(spec.getTemplateUid().getBytes());
//            md.update(Arrays.toString(spec.getCeIds().toArray()).getBytes());
//            md.update(spec.getFrom().toString().getBytes());
//            md.update(spec.getTo().toString().getBytes());
//            byte[] b = md.digest();
//            StringBuilder sb = new StringBuilder();
//            for (int i=0;i<8;i++) sb.append(String.format("%02x", b[i]));
//            return sb.toString();
//        } catch (Exception ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//    public static String sanitizeCol(String name) {
//        String s = name.toLowerCase().replaceAll("[^a-z0-9_]", "_");
//        if (s.length() > 60) s = s.substring(0, 60);
//        if (Character.isDigit(s.charAt(0))) s = "c_" + s;
//        return s;
//    }
//
//    public static String buildCtasSql(String tableName, String templateUid, Instant from, Instant to, List<CeMeta> ces) {
//        if (ces.size() > MAX_CE) throw new IllegalArgumentException("Too many CE in pivot, use per-CE jobs");
//
//        List<String> exprs = new ArrayList<>();
//        for (CeMeta ce : ces) {
//            String colBase = sanitizeCol("ce_" + ce.columnAlias());
//            String when = "CASE WHEN t.canonical_element_id = '" + ce.elementId() + "' THEN ";
//            switch (ce.dataType().name().toUpperCase()) {
//                case "TEXT":
//                    exprs.add("max(" + when + "t.value_text END) AS " + colBase + "_text");
//                    break;
//                case "NUMBER":
//                    exprs.add("max(" + when + "t.value_number END) AS " + colBase + "_num");
//                    break;
//                case "BOOL":
//                    exprs.add("max(" + when + "t.value_text::bool END) AS " + colBase + "_bool");
//                    break;
//                case "REF":
//                default:
//                    exprs.add("max(" + when + "t.value_ref_uid END) AS " + colBase + "_ref_uid");
//                    exprs.add("max(" + when + "t.value_text END) AS " + colBase + "_raw");
//                    break;
//            }
//        }
//
//        String ceExprs = String.join(",\n  ", exprs);
//        String sql = ""
//            + "CREATE TABLE %s\nUSING iceberg\nPARTITIONED BY (submission_day)\nAS\nSELECT\n  t.instance_key,\n  t.submission_uid,\n  t.template_uid,\n  date(t.submission_creation_time) AS submission_day,\n  %s,\n  map_from_entries(collect_list(named_struct('k', t.canonical_element_id, 'v', coalesce(t.value_text, cast(t.value_number as varchar), t.value_json)))) AS attributes\nFROM analytics.tall_canonical t\nWHERE t.template_uid = '%s'\n  AND t.submission_creation_time >= TIMESTAMP '%s'\n  AND t.submission_creation_time <  TIMESTAMP '%s'\nGROUP BY t.instance_key, t.submission_uid, t.template_uid, date(t.submission_creation_time);";
//
//        return String.format(sql, tableName, ceExprs, templateUid, from.toString(), to.toString());
//    }
//}
