package org.nmcpye.datarun.submission.feed.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.config.activation.ActivationSwitches;
import org.nmcpye.datarun.outbox.processing.ChangeBacklog;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Ids;
import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;

/**
 * The first publication (contract 11, feed build step F4): an admin starts it, and the publisher writes every
 * submission's head in id order, a batch at a time (five here), moving its mark past each batch in the batch's own
 * transaction. A restart goes on from the mark, and nothing writes an outbox row. Once it has completed, reconcile
 * repairs what the feed lacks. Values are invented.
 */
@FeedPublisherIntegrationTest
class FirstPublicationIT {

    @Autowired
    private SubmissionPublisher publisher;

    @Autowired
    private ActivationSwitches switches;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeedPublisherProperties properties;

    @Autowired
    private ObjectProvider<SplitPartners> splitPartners;

    @Autowired
    private StoredSubmissionJdbcReader storedSubmissions;

    @Autowired
    private ChangeBacklog backlog;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private FeedFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new FeedFixture(jdbc, transactionManager, url, username, password);
        fixture.ensureConfiguration();
        fixture.clean();
        // a reconcile pass requested at this context's start runs out first, so the batches here are the first
        // publication's
        while (publisher.runBatch()) {
            // the pass, and the retries of whatever it could not publish
        }
        fixture.clean();
    }

    @AfterEach
    void tearDown() {
        fixture.clean();
    }

    /** A publisher as a restart makes it: it knows nothing of the first publication but what the database holds. */
    private SubmissionPublisher restarted() {
        return new SubmissionPublisher(switches, dataSource, transactionManager, objectMapper, properties,
            splitPartners, storedSubmissions, backlog, new SimpleMeterRegistry());
    }

    @Test
    void everyHeadIsWrittenInBatchesFromTheMarkAfterARestartAndNoOutboxRowIsWritten() {
        List<Ids> heads = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            heads.add(fixture.create(document("first " + i, i)));
        }
        long outboxRows = fixture.count("SELECT count(*) FROM outbox", Map.of());

        assertThat(publisher.startFirstPublication()).isPresent();
        assertThat(publisher.startFirstPublication()).as("one is under way: it goes on").isEmpty();
        assertThat(publisher.runBatch()).isTrue();
        FirstPublication.State afterOneBatch = publisher.firstPublicationState().orElseThrow();
        assertThat(afterOneBatch.running()).isTrue();
        assertThat(afterOneBatch.written() + afterOneBatch.failed()).as("one batch").isBetween(1L, 5L);

        SubmissionPublisher restarted = restarted();
        int batches = 0;
        while (restarted.runBatch()) {
            batches++;
        }
        FirstPublication.State done = restarted.firstPublicationState().orElseThrow();

        assertThat(batches).isPositive();
        assertThat(done.running()).isFalse();
        assertThat(done.skipped()).as("no head passed twice: the restart went on from the mark").isZero();
        for (Ids head : heads) {
            assertThat(fixture.record(head)).as(head.id()).containsEntry("version", 0L);
        }
        assertThat(fixture.count("SELECT count(*) FROM outbox", Map.of())).as("no outbox row written")
            .isEqualTo(outboxRows);
        long completed = fixture.count("""
            SELECT count(*) FROM feed.freshness WHERE first_publication_completed_at IS NOT NULL""", Map.of());
        assertThat(completed).as("first_publication_completed_at, once every head it passed was written")
            .isEqualTo(done.failed() == 0 ? 1 : 0);

        assertThat(restarted.startFirstPublication()).as("a finished one starts again from the beginning")
            .hasValueSatisfying(state -> assertThat(state.mark()).isEmpty());
        while (restarted.runBatch()) {
            // the second pass finds every head held
        }
        assertThat(restarted.firstPublicationState().orElseThrow().written()).as("it writes nothing again").isZero();
    }

    /**
     * Once the first publication has completed, reconcile repairs the feed (engineering, 2026-11-26): a head the feed
     * has lost, its record removed here, is published again at the next pass.
     */
    @Test
    void onceTheFirstPublicationHasCompletedReconcileRepairsAHeadTheFeedLacks() {
        List<Ids> heads = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            heads.add(fixture.create(document("first " + i, i)));
        }
        assertThat(publisher.startFirstPublication()).isPresent();
        while (publisher.runBatch()) {
            // the first publication, to its end
        }
        assertThat(publisher.firstPublicationState().orElseThrow().failed())
            .as("every head in the database written (another test may have left one the feed cannot publish)")
            .isZero();
        assertThat(fixture.firstPublicationIsComplete()).isTrue();

        Ids lost = heads.get(3);
        fixture.execute("DELETE FROM feed.submission_row WHERE submission_id = :id", Map.of("id", lost.id()));
        fixture.execute("DELETE FROM feed.submission WHERE id = :id", Map.of("id", lost.id()));
        publisher.reconcileNightly();
        while (publisher.runBatch()) {
            // the pass
        }

        assertThat(fixture.record(lost)).as("published again").containsEntry("version", fixture.head(lost));
        assertThat(fixture.count("SELECT unpublished FROM feed.freshness WHERE feed = 'submissions'", Map.of()))
            .as("the one head the pass found the feed lacking").isEqualTo(1);
    }
}
