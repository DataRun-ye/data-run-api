package org.nmcpye.datarun.jpa.datasubmission.conflict;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.nmcpye.datarun.submission.normalized.SubmissionNormalizer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * The conflict comparison (spec, Conflicts): structural, between two documents without row metadata, together with
 * their deleted flag, assignment and status ({@link ComparedState} says why these, and why not the form version). It does not go through the submission normalizer, and needs no
 * form definition.
 * <ul>
 *   <li>Row metadata is exactly the normalizer's row structure keys ({@link SubmissionNormalizer#ROW_STRUCTURE_KEYS}:
 *       {@code _id}, {@code _index}, {@code _parentId}, {@code _submissionUid}), left out of rows: the objects that are
 *       elements of an array, at any depth. So a copy whose rows lack ids compares equal to the stored state the
 *       legacy route gave ids. Every other key compares, the top-level {@code _status} included.</li>
 *   <li>Numbers compare by value, whatever their spelling: {@code 2.5} equals {@code 2.50}, and {@code 1} equals
 *       {@code 1.0}.</li>
 *   <li>An object's keys compare as a set; an array's elements in order; a JSON null is a value, unlike a missing
 *       key.</li>
 * </ul>
 */
public final class ConflictComparison {

    /**
     * How deep two documents are compared. Request bodies and stored documents both stop at Jackson's nesting limit
     * of 1,000, so this is never reached by data, and the recursion can never exhaust the stack.
     */
    static final int MAX_DEPTH = 1_000;

    private ConflictComparison() {
    }

    /** Whether two states are the same: document, deleted flag, assignment (by id) and status. */
    public static boolean sameState(ComparedState a, ComparedState b) {
        return a.deleted() == b.deleted()
            && Objects.equals(a.assignmentId(), b.assignmentId())
            && Objects.equals(a.status(), b.status())
            && sameDocument(a.document(), b.document());
    }

    /**
     * Whether two upload items are the same phone copy, each as its phone sent it, in either route's shape: the
     * document ({@code formData}), the deleted flag (absent or null is false), the assignment and the status, as sent.
     * Two copies from phones are recognised by what the phones sent; the form version does not count, as for states.
     */
    public static boolean sameUploadItem(JsonNode a, JsonNode b) {
        return sentDeleted(a) == sentDeleted(b)
            && Objects.equals(sentText(a, "assignment"), sentText(b, "assignment"))
            && Objects.equals(sentText(a, "status"), sentText(b, "status"))
            && sameDocument(a.get("formData"), b.get("formData"));
    }

    private static boolean sentDeleted(JsonNode item) {
        JsonNode deleted = item.path("deleted");
        return deleted.isBoolean() && deleted.booleanValue();
    }

    private static String sentText(JsonNode item, String member) {
        JsonNode value = item.get(member);
        return value == null || value.isNull() ? null : value.asText();
    }

    /** Whether two documents are the same, rows' metadata left out. Null is a document holding JSON null. */
    public static boolean sameDocument(JsonNode a, JsonNode b) {
        return same(a == null ? NullNode.getInstance() : a, b == null ? NullNode.getInstance() : b, false, 0);
    }

    private static boolean same(JsonNode a, JsonNode b, boolean row, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException("Documents nested deeper than " + MAX_DEPTH + " are not compared");
        }
        if (a.isNumber() && b.isNumber()) {
            return a.decimalValue().compareTo(b.decimalValue()) == 0;
        }
        if (a.getNodeType() != b.getNodeType()) {
            return false;
        }
        if (a.isObject()) {
            Set<String> keys = keys(a, row);
            if (!keys.equals(keys(b, row))) {
                return false;
            }
            for (String key : keys) {
                if (!same(a.get(key), b.get(key), false, depth + 1)) {
                    return false;
                }
            }
            return true;
        }
        if (a.isArray()) {
            if (a.size() != b.size()) {
                return false;
            }
            for (int i = 0; i < a.size(); i++) {
                if (!same(a.get(i), b.get(i), true, depth + 1)) {
                    return false;
                }
            }
            return true;
        }
        return a.equals(b);
    }

    private static Set<String> keys(JsonNode object, boolean row) {
        Set<String> keys = new HashSet<>();
        for (Iterator<String> names = object.fieldNames(); names.hasNext(); ) {
            String name = names.next();
            if (!row || !SubmissionNormalizer.ROW_STRUCTURE_KEYS.contains(name)) {
                keys.add(name);
            }
        }
        return keys;
    }
}
