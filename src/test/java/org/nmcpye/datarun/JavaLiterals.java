package org.nmcpye.datarun;

import java.util.ArrayList;
import java.util.List;

/**
 * The string literals and text blocks of Java source, for the tests that scan main sources for SQL text
 * ({@code SubmissionWriteOwnershipTest}, {@code FeedWriteOwnershipTest}, {@code NewCodeSeamsTest}). A class of its own,
 * so a scan does not initialize another test class and pay for its class import within its own time limit.
 */
final class JavaLiterals {

    private JavaLiterals() {
    }

    /** A string literal or text block, and whether it sits in a {@code @Query} or {@code @NativeQuery}. */
    record Literal(String text, boolean inQuery) {
    }

    /** The contents of the string literals and text blocks in Java source, comments and char literals skipped. */
    static List<String> strings(String source) {
        return literals(source).stream().map(Literal::text).toList();
    }

    static List<Literal> literals(String source) {
        List<Literal> literals = new ArrayList<>();
        int queryDepth = 0;
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (source.startsWith("//", i)) {
                int end = source.indexOf('\n', i);
                i = end < 0 ? source.length() : end + 1;
            } else if (source.startsWith("/*", i)) {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? source.length() : end + 2;
            } else if (source.startsWith("\"\"\"", i)) {
                int end = source.indexOf("\"\"\"", i + 3);
                end = end < 0 ? source.length() : end;
                literals.add(new Literal(source.substring(i + 3, end), queryDepth > 0));
                i = end + 3;
            } else if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < source.length() && source.charAt(j) != c) {
                    j += source.charAt(j) == '\\' ? 2 : 1;
                }
                if (c == '"') {
                    literals.add(new Literal(source.substring(i + 1, Math.min(j, source.length())), queryDepth > 0));
                }
                i = j + 1;
            } else if (queryDepth == 0 && (source.startsWith("@Query", i) || source.startsWith("@NativeQuery", i))) {
                int open = source.indexOf('(', i);
                boolean annotation = open > 0 && source.substring(i + 1, open).strip().matches("Query|NativeQuery");
                i = annotation ? open + 1 : i + 1;
                queryDepth = annotation ? 1 : 0;
            } else {
                if (queryDepth > 0 && c == '(') {
                    queryDepth++;
                } else if (queryDepth > 0 && c == ')') {
                    queryDepth--;
                }
                i++;
            }
        }
        return literals;
    }
}
