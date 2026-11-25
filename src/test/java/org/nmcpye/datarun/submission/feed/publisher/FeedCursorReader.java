package org.nmcpye.datarun.submission.feed.publisher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * A consumer of the feed's two submission streams the way Airbyte reads them (contract 8, items 1 and 2): each stream
 * has its own cursor; a read takes the stream's records at or after its cursor, keeps the highest position of each key,
 * and moves the cursor to the highest position it saw. A record that committed with a position below a cursor already
 * passed is never read: the reader then holds an older position for that key than the table, which {@link #missed}
 * counts.
 */
final class FeedCursorReader {

    private final Connection connection;
    private long submissionCursor;
    private long rowCursor;
    private final Map<String, Long> seen = new HashMap<>();

    FeedCursorReader(Connection connection) {
        this.connection = connection;
    }

    /** One incremental read of both streams, as one sync would make it. */
    void read() throws SQLException {
        submissionCursor = read("SELECT 's|' || id, position FROM feed.submission WHERE position >= ?",
            submissionCursor);
        rowCursor = read("""
            SELECT 'r|' || submission_id || '|' || id, position FROM feed.submission_row WHERE position >= ?""",
            rowCursor);
    }

    private long read(String sql, long cursor) throws SQLException {
        long highest = cursor;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cursor);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    long position = rows.getLong(2);
                    seen.merge(rows.getString(1), position, Math::max);
                    highest = Math.max(highest, position);
                }
            }
        }
        return highest;
    }

    /** Keys whose current record (the table's) the reader never read: it holds an older position, or none. */
    long missed() throws SQLException {
        long missed = 0;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 's|' || id, position FROM feed.submission
                UNION ALL
                SELECT 'r|' || submission_id || '|' || id, position FROM feed.submission_row""");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                Long read = seen.get(rows.getString(1));
                if (read == null || read != rows.getLong(2)) {
                    missed++;
                }
            }
        }
        return missed;
    }

    long cursor() {
        return Math.max(submissionCursor, rowCursor);
    }
}
