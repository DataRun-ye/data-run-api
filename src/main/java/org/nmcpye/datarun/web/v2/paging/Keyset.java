package org.nmcpye.datarun.web.v2.paging;

import java.util.List;
import java.util.function.Function;

/**
 * Turns the rows of a keyset query into a {@link Page}. The query reads {@link PageQuery#fetchSize()} rows after the
 * cursor's position, ordered by a unique sort key; when it returns more than the limit, a next page exists and its
 * cursor is the key of the last row returned. A keyset page never skips or repeats a row because others were
 * written meanwhile, which offset paging does.
 */
public final class Keyset {

    private Keyset() {
    }

    /**
     * @param rows    the query's rows, at most {@code query.fetchSize()}, in sort order
     * @param query   the page request
     * @param binding the query's binding ({@link QueryBinding})
     * @param keyOf   a row's unique sort key, as cursor parts
     * @param item    a row as the page's item
     */
    public static <R, T> Page<T> page(List<R> rows, PageQuery query, String binding,
                                      Function<R, List<?>> keyOf, Function<R, T> item) {
        if (rows.size() > query.fetchSize()) {
            throw new IllegalArgumentException("A keyset query reads at most limit + 1 rows");
        }
        boolean more = rows.size() > query.limit();
        List<R> shown = more ? rows.subList(0, query.limit()) : rows;
        String next = more ? CursorCodec.encode(binding, keyOf.apply(shown.get(shown.size() - 1))) : null;
        return new Page<>(shown.stream().map(item).toList(), next);
    }
}
