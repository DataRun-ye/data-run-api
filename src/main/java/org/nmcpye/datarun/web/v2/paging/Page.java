package org.nmcpye.datarun.web.v2.paging;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * One page of a v2 list: its items and the cursor of the next page, {@code null} on the last. There are no totals:
 * counting a filtered list means reading all of it.
 *
 * @param items the page's items, in the list's order
 * @param next  the opaque cursor that continues the list after the last item, or {@code null}; always present
 */
public record Page<T>(List<T> items, @JsonInclude(JsonInclude.Include.ALWAYS) String next) {

    public Page {
        items = List.copyOf(items);
    }
}
