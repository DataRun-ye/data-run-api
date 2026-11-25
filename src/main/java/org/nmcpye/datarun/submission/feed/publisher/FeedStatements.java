package org.nmcpye.datarun.submission.feed.publisher;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/**
 * The statements of one feed part, on the caller's transaction, each cancelled once the part's time is up: before it
 * starts, a statement gets the time left (whole seconds, rounded up) as its query timeout, or the transaction's own
 * remaining time when that is shorter; once no time is left it does not start. So the part as a whole ends within its
 * limit, whatever it runs, the form definitions it reads included. PostgreSQL reports a cancelled statement with
 * SQLState 57014.
 */
final class FeedStatements {

    private final NamedParameterJdbcTemplate named;

    FeedStatements(DataSource dataSource, long deadlineNanos) {
        this.named = new NamedParameterJdbcTemplate(new DeadlineJdbcTemplate(dataSource, deadlineNanos));
    }

    NamedParameterJdbcTemplate named() {
        return named;
    }

    private static final class DeadlineJdbcTemplate extends JdbcTemplate {

        private final long deadlineNanos;

        DeadlineJdbcTemplate(DataSource dataSource, long deadlineNanos) {
            super(dataSource);
            this.deadlineNanos = deadlineNanos;
        }

        @Override
        protected void applyStatementSettings(Statement statement) throws SQLException {
            super.applyStatementSettings(statement);
            long left = deadlineNanos - System.nanoTime();
            if (left <= 0) {
                throw new SQLTimeoutException("the feed part's time is up", "57014");
            }
            int seconds = (int) Math.min(Integer.MAX_VALUE, Math.max(1, TimeUnit.NANOSECONDS.toSeconds(left - 1) + 1));
            int transactionLimit = statement.getQueryTimeout();
            if (transactionLimit == 0 || seconds < transactionLimit) {
                statement.setQueryTimeout(seconds);
            }
        }
    }
}
