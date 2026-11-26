package org.nmcpye.datarun.submission.feed.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Ids;
import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader;
import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader.HeadVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;

/**
 * Reconcile reads its pass in pages, and never skips one: a page whose reads fail is read again at the next call, the
 * pass ends only after every page was read, and only then can it count as complete. Each call reads for a bounded time
 * and the next goes on from where it stopped. Seven submissions a page here.
 */
@FeedPublisherIntegrationTest
class ReconcilePagesIT {

    @Autowired
    private StoredSubmissionJdbcReader storedSubmissions;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EtlOrchestrator orchestrator;

    @Autowired
    private PlatformTransactionManager transactionManager;

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
    }

    @AfterEach
    void tearDown() {
        fixture.clean();
    }

    /** The feed's own reads, failing once at the versions the feed holds: the read after the page's cursor moved. */
    private final class FailingOnce extends NamedParameterJdbcTemplate {
        private final AtomicBoolean failed = new AtomicBoolean();

        FailingOnce() {
            super(dataSource);
        }

        @Override
        public void query(String sql, Map<String, ?> paramMap, RowCallbackHandler rch) {
            if (sql.contains("FROM feed.submission") && failed.compareAndSet(false, true)) {
                throw new DataAccessResourceFailureException("injected failure of a page's read");
            }
            super.query(sql, paramMap, rch);
        }
    }

    /** The stored submissions, remembering each page asked for: where it began, and the submissions it held. */
    private static final class RecordingReader extends StoredSubmissionJdbcReader {
        private final List<String> afters = new ArrayList<>();
        private final List<List<String>> pages = new ArrayList<>();

        RecordingReader(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
            super(jdbc, objectMapper);
        }

        @Override
        public List<HeadVersion> headVersionsAfter(String after, int limit) {
            List<HeadVersion> page = super.headVersionsAfter(after, limit);
            afters.add(after);
            pages.add(page.stream().map(HeadVersion::submissionId).toList());
            return page;
        }
    }

    @Test
    void aPageWhoseReadFailsIsReadAgainAndThePassIsNotCompleteBeforeEveryPageWasRead() {
        List<Ids> unpublished = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            unpublished.add(fixture.create(document("unpublished " + i, i)));
        }
        Reconcile reconcile = new Reconcile(storedSubmissions, Duration.ofMinutes(1));
        reconcile.request();
        assertThat(reconcile.begin()).isTrue();
        FailingOnce failing = new FailingOnce();
        TransactionTemplate reading = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> reading.execute(status -> reconcile.next(failing, 7, 5)))
            .hasMessageContaining("injected failure");
        assertThat(reconcile.finishIfDone()).as("not complete after a failed page").isNull();

        List<String> queued = new ArrayList<>();
        Reconcile.Summary summary = null;
        for (int call = 0; call < 10_000 && summary == null; call++) {
            List<String> batch = reading.execute(status -> reconcile.next(failing, 7, 5));
            queued.addAll(batch);
            reconcile.attempted(batch.size(), 0);
            summary = reconcile.finishIfDone();
        }

        assertThat(summary).isNotNull();
        assertThat(queued).as("every unpublished head was queued, the failed page's included")
            .containsAll(unpublished.stream().map(Ids::id).toList())
            .doesNotHaveDuplicates();
        assertThat(summary.complete()).isTrue();
        assertThat(summary.differing()).isEqualTo(queued.size());
    }

    /**
     * Thirty submissions the feed holds: pages with nothing to publish. With its budget spent at once, each call reads
     * one page, never more, and the next call begins where the last page ended, until every submission was read once.
     */
    @Test
    void eachCallReadsWithinItsBudgetAndTheNextGoesOnFromWhereItStopped() {
        List<String> held = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            held.add(fixture.create(document("held " + i, i)).id());
        }
        while (orchestrator.runOnce("feed-test", 50).claimed() > 0) {
            // the feed publishes every change
        }
        RecordingReader pages = new RecordingReader(jdbc, objectMapper);
        Reconcile reconcile = new Reconcile(pages, Duration.ZERO);
        reconcile.request();
        assertThat(reconcile.begin()).isTrue();
        TransactionTemplate reading = new TransactionTemplate(transactionManager);

        Reconcile.Summary summary = null;
        for (int call = 0; call < 10_000 && summary == null; call++) {
            int before = pages.pages.size();
            List<String> batch = reading.execute(status -> reconcile.next(jdbc, 7, 5));
            assertThat(pages.pages.size() - before).as("pages read by one call").isLessThanOrEqualTo(1);
            reconcile.attempted(batch.size(), 0);
            summary = reconcile.finishIfDone();
        }

        assertThat(summary).isNotNull();
        assertThat(pages.afters.get(0)).as("the pass begins before the first submission").isEmpty();
        for (int i = 1; i < pages.pages.size(); i++) {
            List<String> previous = pages.pages.get(i - 1);
            assertThat(pages.afters.get(i)).as("page %d begins where page %d ended", i, i - 1)
                .isEqualTo(previous.get(previous.size() - 1));
        }
        List<String> read = pages.pages.stream().flatMap(List::stream).toList();
        assertThat(read).as("every submission read once").doesNotHaveDuplicates().containsAll(held)
            .hasSize((int) fixture.count("SELECT count(*) FROM data_submission", Map.of()));
    }
}
