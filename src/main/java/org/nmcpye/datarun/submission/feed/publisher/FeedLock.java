package org.nmcpye.datarun.submission.feed.publisher;

import org.springframework.jdbc.core.JdbcOperations;

/**
 * The submission feed's lock (contract 8, item 1): every transaction that writes {@code feed.submission},
 * {@code feed.submission_row}, {@code feed.freshness} or {@code feed_ops} takes it first, as a transaction-scoped
 * PostgreSQL advisory lock, and only then reads heads and takes positions. Commits therefore happen in position order,
 * whoever writes: the ETL's live changes, the retries, the republication and reconcile, and two publishers if two ever
 * run. The ETL's run lock is not relied on: it is held on a connection apart from the run's transactions, which can be
 * lost while the run goes on.
 *
 * <p>Its key is not the dictionary's ({@link DictionaryPublisher#LOCK_KEY}): the dictionary is rewritten on its own
 * schedule, and neither waits for the other.</p>
 */
final class FeedLock {

    /** "feed" in ASCII, then 2. */
    static final long KEY = 0x6665_6564_0000_0002L;

    private static final String TAKE = "SELECT pg_advisory_xact_lock(" + KEY + ")";

    private FeedLock() {
    }

    /** Takes the lock in the caller's transaction, waiting for whoever holds it; released at commit or rollback. */
    static void take(JdbcOperations jdbc) {
        jdbc.execute(TAKE);
    }
}
