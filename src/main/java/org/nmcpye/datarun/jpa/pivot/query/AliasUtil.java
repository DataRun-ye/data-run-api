//package org.nmcpye.datarun.jpa.pivot.query;
//
///**
// * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
// */
//public final class AliasUtil {
//    private AliasUtil() {
//    }
//
//    /**
//     * Deterministic alias generator for pivot mapping IDs.
//     * e.g. "element.eCw9HcbcnW7" -> "element_eCw9HcbcnW7"
//     */
//    public static String alias(String id) {
//        if (id == null) return null;
//        // replace any non-alphanumeric char with underscore, collapse multiple underscores
//        String s = id.replaceAll("[^A-Za-z0-9]", "_");
//        s = s.replaceAll("_+", "_");
//        // ensure doesn't start with digit
//        if (!s.isEmpty() && Character.isDigit(s.charAt(0))) {
//            s = "_" + s;
//        }
//        return s;
//    }
//}
