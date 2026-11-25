package org.nmcpye.datarun.submission.feed.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.nmcpye.datarun.config.activation.ActivationSwitches;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Change;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Ids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.MISSING_ASSIGNMENT_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.VERSION_2_UID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.row;

/**
 * Positions commit in order, whoever writes the feed, and a reader whose cursor moves forward misses nothing (contract
 * 8, item 1). Every feed write here is slowed inside its transaction, so commits would overtake one another without
 * the feed's lock, and a reader polls the two streams throughout, as Airbyte does. Values are invented.
 */
@FeedPublisherIntegrationTest
class ConcurrentPublishersIT {

    @Autowired
    private EtlOrchestrator orchestrator;

    @Autowired
    private SubmissionPublisher publisher;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ActivationSwitches switches;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeedPublisherProperties properties;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private FeedFixture fixture;
    private ExecutorService threads;

    @BeforeEach
    void setUp() {
        fixture = new FeedFixture(jdbc, transactionManager, url, username, password);
        fixture.ensureConfiguration();
        fixture.clean();
        // the pass requested at this context's start, over whatever the shared database held, runs out first
        while (publisher.runBatch()) {
            // nothing to check
        }
        fixture.clean();
        threads = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() throws Exception {
        threads.shutdownNow();
        threads.awaitTermination(10, TimeUnit.SECONDS);
        slowFeedWrites(false);
        fixture.unfixPlacement();
        fixture.clean();
    }

    /** Makes every write of a submission record sleep a few milliseconds inside its transaction, or stops it. */
    private void slowFeedWrites(boolean slow) {
        fixture.execute("DROP TRIGGER IF EXISTS fd_test_slow ON feed.submission", Map.of());
        if (slow) {
            fixture.execute("""
                CREATE OR REPLACE FUNCTION fd_test_slow() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                  PERFORM pg_sleep(0.002 + random() * 0.008);
                  RETURN NEW;
                END $$""", Map.of());
            fixture.execute("""
                CREATE TRIGGER fd_test_slow AFTER INSERT OR UPDATE ON feed.submission
                FOR EACH ROW EXECUTE FUNCTION fd_test_slow()""", Map.of());
        }
    }

    private SubmissionPublisher secondPublisher() {
        return new SubmissionPublisher(switches, dataSource, transactionManager, properties,
            new FeedWriter(new FormDefinitions(), objectMapper, SplitPartners.NONE), new SimpleMeterRegistry());
    }

    /** Runs the task over and over on a thread of its own until {@code stop}, then once more. */
    private Future<Integer> repeat(AtomicBoolean stop, Callable<Boolean> task) {
        return threads.submit(() -> {
            int rounds = 0;
            boolean more = true;
            while (!stop.get() || more) {
                more = task.call();
                rounds++;
                if (!more) {
                    Thread.sleep(2);
                }
            }
            return rounds;
        });
    }

    private long fixtureHeadsUnpublished() {
        return fixture.count("""
            SELECT count(*) FROM data_submission s
            JOIN LATERAL (SELECT max(version) AS version FROM journal.submission_revision r
                          WHERE r.submission_id = s.id) h ON true
            LEFT JOIN feed.submission f ON f.id = s.id
            WHERE s.id LIKE :prefix AND f.version IS DISTINCT FROM h.version""",
            Map.of("prefix", FeedFixture.ID_PREFIX + "%"));
    }

    private long positionsUsedTwice() {
        return fixture.count("""
            SELECT count(*) FROM (SELECT x.position
                                  FROM (SELECT position FROM feed.submission
                                        UNION ALL SELECT position FROM feed.submission_row) x
                                  GROUP BY x.position HAVING count(*) > 1) d""", Map.of());
    }

    /**
     * Two ETL loops publishing live rows, and two publishers' deferred work (the application's and a second
     * process's, each with its own reconcile pass) on pending retries, republish marks and reconcile batches, all at
     * once, every feed write slowed: the reader's view ends equal to the tables, every head is published once, and no
     * position is used twice.
     */
    @Test
    @Timeout(180)
    void twoPublishersAtOnceAcrossEveryKindOfFeedWriteCommitInPositionOrder() throws Exception {
        List<Ids> republished = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            republished.add(fixture.create(document("republished " + i, i, row("r" + i, "x"))));
        }
        while (orchestrator.runOnce("feed-test", 50).claimed() > 0) {
            // published before the run
        }
        for (Ids submission : republished) {
            fixture.execute("INSERT INTO feed_ops.republish (submission_id, marked_at) VALUES (:id, now())",
                Map.of("id", submission.id()));
        }
        List<Ids> retried = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Ids submission = FeedFixture.nextIds();
            fixture.create(submission, document("retried " + i, i, row("r" + i, "x")), VERSION_2_UID,
                MISSING_ASSIGNMENT_UID);
            retried.add(submission);
        }
        while (orchestrator.runOnce("feed-test", 50).claimed() > 0) {
            // their feed parts fail: pending changes
        }
        fixture.execute("UPDATE feed_ops.pending SET next_attempt_at = now()", Map.of());
        fixture.fixPlacement();
        List<Ids> reconciled = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            reconciled.add(fixture.create(document("reconciled " + i, i, row("r" + i, "x"))));
        }
        fixture.execute("UPDATE outbox SET status = 'success' WHERE status = 'pending'", Map.of());
        List<Ids> live = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < 20; i++) {
            live.add(fixture.create(document("live " + i, i, row("r" + i, "x"), row("s" + i, "y"))));
        }
        slowFeedWrites(true);
        publisher.reconcileNightly();
        SubmissionPublisher second = secondPublisher();
        second.reconcileAtStart();

        AtomicBoolean stop = new AtomicBoolean();
        long missed;
        try (Connection readerConnection = fixture.connect()) {
            FeedCursorReader reader = new FeedCursorReader(readerConnection);
            List<Future<Integer>> publishers = List.of(
                repeat(stop, () -> orchestrator.runOnce("feed-test-a", 3).claimed() > 0),
                repeat(stop, () -> orchestrator.runOnce("feed-test-b", 3).claimed() > 0),
                repeat(stop, publisher::runBatch),
                repeat(stop, second::runBatch));
            Future<?> writer = threads.submit(() -> {
                for (int round = 0; round < 3; round++) {
                    for (Ids submission : live) {
                        fixture.update(submission, document("live again " + round, round,
                            row("r" + round, "x"), row("t" + ThreadLocalRandom.current().nextInt(3), "z")));
                    }
                }
                return null;
            });
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120);
            while (!writer.isDone() && System.nanoTime() < deadline) {
                reader.read();
            }
            writer.get();
            stop.set(true);
            for (Future<Integer> running : publishers) {
                while (!running.isDone()) {
                    reader.read();
                }
                assertThat(running.get()).isPositive();
            }
            reader.read();
            missed = reader.missed();
        }

        assertThat(missed).as("records the reader's cursor passed before they committed").isZero();
        assertThat(positionsUsedTwice()).isZero();
        assertThat(fixtureHeadsUnpublished()).as("every head published at its version").isZero();
        assertThat(fixture.count("SELECT count(*) FROM feed_ops.republish", Map.of())).as("marks written").isZero();
        for (Ids submission : retried) {
            assertThat(fixture.pending(submission)).as("retried and published").isEmpty();
        }
    }

    /**
     * The reader's check catches commits out of position order: two sessions writing feed positions without the
     * feed's lock, the later position committed first, make the reader miss the earlier one. With the lock held by the
     * first session, the ETL's publication waits for it, and its higher positions commit after.
     */
    @Test
    @Timeout(60)
    void withoutTheLockACommitOutOfOrderIsMissedAndWithItThePublicationWaits() throws Exception {
        Ids first = fixture.create(document("first", 1));
        Ids second = fixture.create(document("second", 1));
        while (orchestrator.runOnce("feed-test", 50).claimed() > 0) {
            // both published
        }

        try (Connection readerConnection = fixture.connect();
             Connection early = fixture.connect();
             Connection late = fixture.connect()) {
            FeedCursorReader reader = new FeedCursorReader(readerConnection);
            reader.read();
            early.setAutoCommit(false);
            late.setAutoCommit(false);
            rewrite(early, first);
            rewrite(late, second);
            late.commit();
            reader.read();
            early.commit();
            reader.read();
            assertThat(reader.missed()).as("without the lock, the earlier position committed late is missed")
                .isEqualTo(1);
        }

        Ids third = fixture.create(document("third", 1));
        try (Connection readerConnection = fixture.connect(); Connection holder = fixture.connect()) {
            FeedCursorReader reader = new FeedCursorReader(readerConnection);
            reader.read();
            holder.setAutoCommit(false);
            try (PreparedStatement lock = holder.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                lock.setLong(1, FeedLock.KEY);
                lock.executeQuery().close();
            }
            rewrite(holder, first);
            Future<?> etl = threads.submit(() -> orchestrator.runOnce("feed-test", 10));
            Thread.sleep(500);
            assertThat(etl.isDone()).as("the publication waits for the lock").isFalse();
            reader.read();
            holder.commit();
            etl.get(10, TimeUnit.SECONDS);
            reader.read();
            assertThat(reader.missed()).as("with the lock, nothing is missed").isZero();
        }
        assertThat(fixture.record(third)).containsEntry("version", 0L);
    }

    /** Rewrites the submission's record with a new position, in the session's open transaction. */
    private static void rewrite(Connection session, Ids submission) throws Exception {
        try (PreparedStatement update = session.prepareStatement(
            "UPDATE feed.submission SET position = nextval('feed.position_seq') WHERE id = ?")) {
            update.setString(1, submission.id());
            assertThat(update.executeUpdate()).isEqualTo(1);
        }
    }

    /**
     * Writers commit concurrently, a third of their changes held open before committing, so outbox rows commit out of
     * id order: the case the feed spike's own claim missed (a late commit below its id mark was never claimed). The
     * ETL claims by status, so every change is published, and the reader misses nothing.
     */
    @Test
    @Timeout(180)
    void concurrentWritersWithLateCommitsAndAReaderMissNothing() throws Exception {
        AtomicBoolean writersDone = new AtomicBoolean();
        List<Ids> written = Collections.synchronizedList(new ArrayList<>());
        long missed;
        try (Connection readerConnection = fixture.connect()) {
            FeedCursorReader reader = new FeedCursorReader(readerConnection);
            List<Future<?>> writers = new ArrayList<>();
            for (int w = 0; w < 4; w++) {
                int writerNo = w;
                writers.add(threads.submit(() -> {
                    for (int i = 0; i < 12; i++) {
                        Ids ids = FeedFixture.nextIds();
                        String doc = document("w" + writerNo + " " + i, i, row("r" + i, "x"));
                        if (i % 3 == 0) {
                            try (Change change = fixture.createOpen(ids, doc)) {
                                Thread.sleep(50 + ThreadLocalRandom.current().nextInt(150));
                                change.commit();
                            }
                        } else {
                            fixture.create(ids, doc, VERSION_2_UID, FeedFixture.ASSIGNMENT_UID);
                        }
                        written.add(ids);
                        if (i % 4 == 1) {
                            fixture.update(ids, document("w" + writerNo + " again " + i, i, row("r" + i, "y")));
                        }
                    }
                    return null;
                }));
            }
            Future<Integer> etl = repeat(writersDone, () -> orchestrator.runOnce("feed-test", 5).claimed() > 0);
            for (Future<?> writer : writers) {
                while (!writer.isDone()) {
                    reader.read();
                }
                writer.get();
            }
            writersDone.set(true);
            while (!etl.isDone()) {
                reader.read();
            }
            etl.get();
            while (orchestrator.runOnce("feed-test", 50).claimed() > 0) {
                reader.read();
            }
            reader.read();
            missed = reader.missed();
        }

        assertThat(written).hasSize(48);
        assertThat(fixtureHeadsUnpublished()).as("no change stranded").isZero();
        assertThat(missed).as("records the reader's cursor passed before they committed").isZero();
        assertThat(positionsUsedTwice()).isZero();
    }
}
