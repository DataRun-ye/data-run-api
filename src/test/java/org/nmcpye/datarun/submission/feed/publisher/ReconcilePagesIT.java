package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.submission.feed.publisher.FeedFixture.Ids;
import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.document;

/**
 * Reconcile reads its pass in pages, and never skips one: a page whose reads fail is read again at the next call, the
 * pass ends only after every page was read, and only then can it count as complete. Seven submissions a page here.
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

    @Test
    void aPageWhoseReadFailsIsReadAgainAndThePassIsNotCompleteBeforeEveryPageWasRead() {
        List<Ids> unpublished = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            unpublished.add(fixture.create(document("unpublished " + i, i)));
        }
        Reconcile reconcile = new Reconcile(storedSubmissions);
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
}
