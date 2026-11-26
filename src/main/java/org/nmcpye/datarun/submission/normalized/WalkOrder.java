package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * The order the submission normalizer walks a stored document in, stated once (contract 5, "earlier means walk
 * order"): depth first from the root; each object's keys in {@link String#compareTo} order, by UTF-16 code unit; each
 * array in stored order; a row before its nested rows. The normalizer walks objects by {@link #keys}, and whatever must
 * know which of two places it meets first compares their data paths by {@link #PATHS}, so the two cannot part.
 */
public final class WalkOrder {

    /** An object's keys, in the order they are walked. */
    public static final Comparator<String> KEYS = Comparator.naturalOrder();

    /**
     * Data paths in the order the walk meets them: key by key from the root, each key in {@link #KEYS} order, a path
     * before the paths under it.
     */
    public static final Comparator<String> PATHS = (left, right) -> {
        String[] a = left.split("\\.", -1);
        String[] b = right.split("\\.", -1);
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            int order = KEYS.compare(a[i], b[i]);
            if (order != 0) {
                return order;
            }
        }
        return Integer.compare(a.length, b.length);
    };

    private WalkOrder() {
    }

    /** The object's keys, in the order they are walked. */
    public static List<String> keys(JsonNode object) {
        List<String> keys = new ArrayList<>(object.size());
        for (Iterator<String> names = object.fieldNames(); names.hasNext(); ) {
            keys.add(names.next());
        }
        keys.sort(KEYS);
        return keys;
    }
}
