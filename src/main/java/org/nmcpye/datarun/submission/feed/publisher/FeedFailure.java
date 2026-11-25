package org.nmcpye.datarun.submission.feed.publisher;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.TransactionTimedOutException;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;

/**
 * Why a submission could not be published, as a reason code: what a pending change and a dead one keep, and what the
 * logs show. A code never holds a submitted value, nor any text of the failure, which can quote one (a database
 * error's detail repeats the row it refused).
 *
 * <ul>
 *   <li>{@value #HEAD_MISSING}: the submission has no revision in the journal, or no stored row;</li>
 *   <li>{@value #FORM_UNRESOLVED}: its form uid names no stored form;</li>
 *   <li>{@value #PLACEMENT_UNRESOLVED}: a uid of its placement (assignment, team, org unit, activity) names no stored
 *       row, so the record would lack a column the contract requires;</li>
 *   <li>{@value #TIMEOUT}: the feed part ran out of time;</li>
 *   <li>{@code DATABASE_<SQLState>}: any other database error, by its SQLState;</li>
 *   <li>{@value #UNEXPECTED}: anything else.</li>
 * </ul>
 */
final class FeedFailure extends RuntimeException {

    static final String HEAD_MISSING = "HEAD_MISSING";
    static final String FORM_UNRESOLVED = "FORM_UNRESOLVED";
    static final String PLACEMENT_UNRESOLVED = "PLACEMENT_UNRESOLVED";
    static final String TIMEOUT = "TIMEOUT";
    static final String UNEXPECTED = "UNEXPECTED";

    /** PostgreSQL's query_canceled: a statement timeout or a query timeout. */
    private static final String QUERY_CANCELED = "57014";

    private static final int MAX_CAUSES = 20;

    private final String reason;

    FeedFailure(String reason) {
        super(reason, null, false, false);
        this.reason = reason;
    }

    String reason() {
        return reason;
    }

    /** The reason code of any failure of a feed part. */
    static String reasonOf(Throwable failure) {
        SQLException deepest = null;
        int depth = 0;
        for (Throwable t = failure; t != null && depth++ < MAX_CAUSES; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof FeedFailure feedFailure) {
                return feedFailure.reason;
            }
            if (t instanceof QueryTimeoutException || t instanceof TransactionTimedOutException
                || t instanceof SQLTimeoutException) {
                return TIMEOUT;
            }
            if (t instanceof SQLException sql) {
                deepest = sql;
            }
        }
        if (deepest != null) {
            SQLException next = deepest.getNextException();
            String state = next != null && next.getSQLState() != null ? next.getSQLState() : deepest.getSQLState();
            if (QUERY_CANCELED.equals(state)) {
                return TIMEOUT;
            }
            if (state != null && state.matches("[0-9A-Z]{5}")) {
                return "DATABASE_" + state;
            }
        }
        return UNEXPECTED;
    }

    /** The failure's type, for a log line, without its message: a message can quote a submitted value. */
    static String typeOf(Throwable failure) {
        Throwable root = failure;
        int depth = 0;
        while (root.getCause() != null && root.getCause() != root && depth++ < MAX_CAUSES) {
            root = root.getCause();
        }
        return root == failure ? failure.getClass().getSimpleName()
            : failure.getClass().getSimpleName() + " caused by " + root.getClass().getSimpleName();
    }
}
