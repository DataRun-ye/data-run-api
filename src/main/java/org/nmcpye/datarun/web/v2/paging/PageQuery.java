package org.nmcpye.datarun.web.v2.paging;

import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;
import org.nmcpye.datarun.web.v2.problem.ProblemError;

import java.util.Optional;

/**
 * A list request's paging: {@code limit} (default {@value #DEFAULT_LIMIT}, at most {@value #MAX_LIMIT}) and the
 * opaque {@code cursor} of the page to continue after. A v2 handler takes it as a parameter; it is read from the
 * query string, and the OpenAPI document lists both parameters.
 *
 * @param limit  items per page
 * @param cursor the cursor as sent, or {@code null} for the first page
 */
public record PageQuery(int limit, String cursor) {

    public static final String LIMIT = "limit";
    public static final String CURSOR = "cursor";
    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    /**
     * From the query parameters as sent.
     *
     * @throws ApiProblemException 400 {@code bad-request} for a limit that is not a whole number from 1 to
     *                             {@value #MAX_LIMIT}; 400 {@code invalid-cursor} for a cursor longer than any
     *                             cursor is
     */
    public static PageQuery of(String limit, String cursor) {
        int size = DEFAULT_LIMIT;
        if (limit != null && !limit.isEmpty()) {
            try {
                size = Integer.parseInt(limit);
            } catch (NumberFormatException e) {
                size = -1;
            }
            if (size < 1 || size > MAX_LIMIT) {
                throw ApiProblemException.of(ProblemCode.BAD_REQUEST)
                    .detail("limit must be a whole number from 1 to " + MAX_LIMIT + ".")
                    .error(ProblemError.atParameter(LIMIT, "out-of-range", "From 1 to " + MAX_LIMIT + "."));
            }
        }
        String token = cursor == null || cursor.isEmpty() ? null : cursor;
        if (token != null && token.length() > CursorCodec.MAX_LENGTH) {
            throw CursorCodec.invalid();
        }
        return new PageQuery(size, token);
    }

    /**
     * The position to continue after, decoded against this query's binding; empty for the first page.
     *
     * @throws ApiProblemException 400 {@code invalid-cursor} when the cursor belongs to another query or is not a
     *                             cursor at all
     */
    public Optional<Cursor> after(String binding) {
        return cursor == null ? Optional.empty() : Optional.of(CursorCodec.decode(cursor, binding));
    }

    /** How many rows a query reads: one more than the page, so it knows whether another page follows. */
    public int fetchSize() {
        return limit + 1;
    }
}
