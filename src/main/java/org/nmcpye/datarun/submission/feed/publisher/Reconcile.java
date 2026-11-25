package org.nmcpye.datarun.submission.feed.publisher;

import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader;
import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader.HeadVersion;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.ArrayDeque;
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

    Reconcile(StoredSubmissionJdbcReader reader) {
        this.reader = reader;
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
     * The next submissions to publish, at most {@code limit}, scanning further while none are queued; empty once the
     * pass has scanned every submission and published its queue. The scan reads in the caller's transaction.
     */
    List<String> next(NamedParameterJdbcTemplate jdbc, int scanSize, int limit) {
        while (queue.isEmpty() && !scannedAll) {
            scan(jdbc, scanSize);
        }
        return queue.stream().limit(limit).toList();
    }

    private void scan(NamedParameterJdbcTemplate jdbc, int scanSize) {
        List<HeadVersion> page = reader.headVersionsAfter(after, scanSize);
        if (page.size() < scanSize) {
            scannedAll = true;
        }
        if (page.isEmpty()) {
            return;
        }
        after = page.get(page.size() - 1).submissionId();
        Map<String, Object> ids = Map.of("ids", page.stream().map(HeadVersion::submissionId).toList());
        Map<String, Long> held = new HashMap<>();
        jdbc.query(HELD, ids, rs -> {
            held.put(rs.getString("id"), rs.getLong("version"));
        });
        Map<String, Boolean> pending = new HashMap<>();
        jdbc.query(PENDING, ids, rs -> {
            pending.put(rs.getString("submission_id"), rs.getBoolean("dead"));
        });
        for (HeadVersion head : page) {
            if (head.version() == null) {
                continue;
            }
            checked++;
            if (Objects.equals(head.version(), held.get(head.submissionId()))) {
                continue;
            }
            differing++;
            if (Boolean.FALSE.equals(pending.get(head.submissionId()))) {
                waiting++;
            } else {
                queue.add(head.submissionId());
            }
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
