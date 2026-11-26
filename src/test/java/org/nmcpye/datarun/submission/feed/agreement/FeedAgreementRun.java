package org.nmcpye.datarun.submission.feed.agreement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the agreement harness ({@link FeedAgreement}) through Maven on the database the environment names, for a local
 * database only: anything run against the lab runs on a lab host, from the bundle ({@link FeedAgreementMain},
 * {@code scripts/feed/agreement.sh}). Its name matches no suite's pattern, so no suite runs it, and without
 * {@code FEED_AGREEMENT_JDBC_URL} it is skipped. The counts go to {@code target/feed-agreement.txt}; it fails unless
 * every agreement line reads 0.
 */
@EnabledIfEnvironmentVariable(named = "FEED_AGREEMENT_JDBC_URL", matches = ".+")
class FeedAgreementRun {

    @Test
    @Timeout(value = 6, unit = TimeUnit.HOURS)
    void everyHeadAgreesWithTheOracle() throws Exception {
        FeedAgreement.Report report = run(System.getenv(), Path.of("target", "feed-agreement.txt"));
        assertThat(report.agrees()).as("every agreement line reads 0 (the counts: target/feed-agreement.txt)")
            .isTrue();
    }

    /** Measures the database the environment names and writes the counts to {@code out}. */
    static FeedAgreement.Report run(Map<String, String> environment, Path out) throws Exception {
        return FeedAgreementMain.measure(environment, out);
    }
}
