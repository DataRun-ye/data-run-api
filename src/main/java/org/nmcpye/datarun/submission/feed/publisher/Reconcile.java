package org.nmcpye.datarun.submission.feed.publisher;

import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader;
import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader.HeadVersion;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reconcile (contract 8, item 4): a pass over every submission, in id order, comparing its head's version with the
 * version the feed holds. A submission the feed lacks or holds at another version is queued for publication, which the
 * publisher does in batches behind the live backlog, each under the feed's lock and reading the head again. That covers
 * periods with the feed off, a rollback to a release without it, dead outbox rows, and anything else that left a head
 * unpublished.
 *
 * <p>The submissions and their heads' versions come from the stored-submission read seam
 * ({@link StoredSubmissionJdbcReader#headVersionsAfter}); the versions the feed holds and the pending changes, from the
 * feed's own tables.</p>
 *
 * <p>The pass reads a page at a time, and each call reads for a bounded time: at least one page, then more only while
 * nothing is queued and its scan budget lasts, so a stretch where every head is held never keeps one call reading the
 * whole table. The next call goes on from where the last one stopped.</p>
 *
 * <p>A pass is requested at start (the switch is read at start, so this is also switch-on) and nightly; a request
 * during a pass starts another after it. A submission with a pending change that is not dead is counted but left to
 * its retries; a dead one is tried, so a fixed cause gets it published. A submission with no revision has no head to
 * publish (integrity check 11 counts it). The pass is held in memory: a restart starts a new one, which skips every
 * head already held.</p>
 *
 * <p>Not safe for concurrent use: the ETL's poll runs it, one batch at a time.</p>
 */
final class Reconcile {

    /** The versions the feed holds of some submissions. */
    private static final String HELD = "SELECT id, version FROM feed.submission WHERE id IN (:ids)";

    /** Whether some submissions' pending changes are dead. */
    private static final String PENDING = """
        SELECT submission_id, dead_at IS NOT NULL AS dead FROM feed_ops.pending WHERE submission_id IN (:ids)""";

    /**
     * What a finished pass found.
     *
     * @param checked   submissions compared with their head
     * @param differing those the feed lacked or held at another version: the feed's {@code unpublished}
     * @param published those published by the pass
     * @param failed    those whose publication failed, now pending changes
     * @param waiting   those left to the retries of their pending change
     */
    record Summary(long checked, long differing, long published, long failed, long waiting) {

        /** Whether the pass wrote every head the feed lacked or held at another version. */
        boolean complete() {
            return failed == 0 && waiting == 0;
        }
    }

    private final StoredSubmissionJdbcReader reader;
    private final Duration scanBudget;
    private final AtomicBoolean requested = new AtomicBoolean();

    private boolean running;
    private String after;
    private boolean scannedAll;
    private final Deque<String> queue = new ArrayDeque<>();
    private long checked;
    private long differing;
    private long published;
    private long failed;
    private long waiting;

    /**
     * @param scanBudget how long one call may go on reading pages while nothing is queued, after its first page
     */
    Reconcile(StoredSubmissionJdbcReader reader, Duration scanBudget) {
        this.reader = reader;
        this.scanBudget = scanBudget;
    }

    /** Requests a pass: the next batch behind the live backlog starts one, or the next after the current pass. */
    void request() {
        requested.set(true);
    }

    /** Starts a pass if one was requested and none runs; whether a pass runs. */
    boolean begin() {
        if (!running && requested.getAndSet(false)) {
            running = true;
            after = "";
            scannedAll = false;
            queue.clear();
            checked = 0;
            differing = 0;
            published = 0;
            failed = 0;
            waiting = 0;
        }
        return running;
    }

    /**
     * The next submissions to publish, at most {@code limit}. While none are queued it reads on, a page of
     * {@code scanSize} at a time: at least one page, then more while nothing is queued, until the scan budget is spent
     * or every submission was read. Empty when the budget ran out first, and the next call goes on from the same place;
     * empty also once the pass has read every submission and published its queue. The reads run in the caller's
     * transaction.
     */
    List<String> next(NamedParameterJdbcTemplate jdbc, int scanSize, int limit) {
        if (queue.isEmpty() && !scannedAll) {
            long stopAt = System.nanoTime() + scanBudget.toNanos();
            do {
                scan(jdbc, scanSize);
            } while (queue.isEmpty() && !scannedAll && System.nanoTime() - stopAt < 0);
        }
        return queue.stream().limit(limit).toList();
    }

    /**
     * Reads the next page and queues what differs. The pass moves past the page only once all of the page's reads
     * have succeeded: a read that fails leaves the cursor, the counts and the queue as they were, and the same page is
     * read again at the next call. So a pass ends, and can count as complete, only after every page was read.
     */
    private void scan(NamedParameterJdbcTemplate jdbc, int scanSize) {
        List<HeadVersion> page = reader.headVersionsAfter(after, scanSize);
        Map<String, Long> held = new HashMap<>();
        Map<String, Boolean> pending = new HashMap<>();
        if (!page.isEmpty()) {
            Map<String, Object> ids = Map.of("ids", page.stream().map(HeadVersion::submissionId).toList());
            jdbc.query(HELD, ids, rs -> {
                held.put(rs.getString("id"), rs.getLong("version"));
            });
            jdbc.query(PENDING, ids, rs -> {
                pending.put(rs.getString("submission_id"), rs.getBoolean("dead"));
            });
        }
        long pageChecked = 0;
        long pageDiffering = 0;
        long pageWaiting = 0;
        List<String> pageQueue = new ArrayList<>();
        for (HeadVersion head : page) {
            if (head.version() == null) {
                continue;
            }
            pageChecked++;
            if (Objects.equals(head.version(), held.get(head.submissionId()))) {
                continue;
            }
            pageDiffering++;
            if (Boolean.FALSE.equals(pending.get(head.submissionId()))) {
                pageWaiting++;
            } else {
                pageQueue.add(head.submissionId());
            }
        }
        checked += pageChecked;
        differing += pageDiffering;
        waiting += pageWaiting;
        queue.addAll(pageQueue);
        if (!page.isEmpty()) {
            after = page.get(page.size() - 1).submissionId();
        }
        if (page.size() < scanSize) {
            scannedAll = true;
        }
    }

    /** Counts the first {@code attempted} queued submissions as done, {@code failedOnes} of them failed. */
    void attempted(int attempted, int failedOnes) {
        for (int i = 0; i < attempted && !queue.isEmpty(); i++) {
            queue.poll();
        }
        published += attempted - failedOnes;
        failed += failedOnes;
    }

    /** Ends the pass once it has nothing left to publish; its summary, or {@code null} while it goes on. */
    Summary finishIfDone() {
        if (!running || !scannedAll || !queue.isEmpty()) {
            return null;
        }
        running = false;
        return new Summary(checked, differing, published, failed, waiting);
    }
}
