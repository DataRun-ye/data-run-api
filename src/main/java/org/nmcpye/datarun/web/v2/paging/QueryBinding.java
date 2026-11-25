package org.nmcpye.datarun.web.v2.paging;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * What binds a cursor to its query: a short digest of the list's name and its filters, normalized so equal filters
 * bind equally (filter names sorted, each filter's values sorted). The limit and the cursor are not filters: a
 * client may change the page size between pages.
 */
public final class QueryBinding {

    private QueryBinding() {
    }

    /**
     * @param list    the list's stable name, for example {@code "things"}, so a cursor of one list fails on another
     * @param filters the filters by name, with their values; absent filters are left out or given no values
     */
    public static String of(String list, Map<String, ? extends Collection<String>> filters) {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
        update(sha256, list);
        for (Map.Entry<String, ? extends Collection<String>> filter : new TreeMap<>(filters).entrySet()) {
            List<String> values = new ArrayList<>(filter.getValue() == null ? List.of() : filter.getValue());
            if (values.isEmpty()) {
                continue;
            }
            values.sort(null);
            update(sha256, filter.getKey());
            sha256.update(ByteBuffer.allocate(4).putInt(values.size()).array());
            values.forEach(value -> update(sha256, value));
        }
        byte[] digest = sha256.digest();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(java.util.Arrays.copyOf(digest, 12));
    }

    /** Each part length-prefixed, so no two different inputs run together into the same bytes. */
    private static void update(MessageDigest digest, String part) {
        byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
