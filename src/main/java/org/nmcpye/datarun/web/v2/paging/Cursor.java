package org.nmcpye.datarun.web.v2.paging;

import org.nmcpye.datarun.web.v2.problem.ApiProblemException;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;

/**
 * A decoded keyset position: the sort key of the last item a page returned, in the list's sort order. The next
 * page reads the items after it. A position holds sort keys only, never a filter, so it grants nothing: the query
 * that reads after it still applies the caller's filters and access.
 *
 * @param keys the sort key's parts: {@link Long} or {@link String}
 */
public record Cursor(List<Object> keys) {

    public Cursor {
        keys = List.copyOf(keys);
    }

    public int size() {
        return keys.size();
    }

    /** @throws ApiProblemException 400 {@code invalid-cursor} when that part is not a number */
    public long longAt(int index) {
        if (index < keys.size() && keys.get(index) instanceof Long value) {
            return value;
        }
        throw CursorCodec.invalid();
    }

    /** @throws ApiProblemException 400 {@code invalid-cursor} when that part is not a string */
    public String stringAt(int index) {
        if (index < keys.size() && keys.get(index) instanceof String value) {
            return value;
        }
        throw CursorCodec.invalid();
    }

    /** @throws ApiProblemException 400 {@code invalid-cursor} when that part is not an instant */
    public Instant instantAt(int index) {
        try {
            return Instant.parse(stringAt(index));
        } catch (DateTimeException e) {
            throw CursorCodec.invalid();
        }
    }
}
