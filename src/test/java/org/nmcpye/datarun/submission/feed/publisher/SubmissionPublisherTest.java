package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** The feed part's time limit always stays below the ETL row's. */
class SubmissionPublisherTest {

    @Test
    void theSettingIsKeptWhileItStaysBelowTheRowsTimeout() {
        assertThat(SubmissionPublisher.partTimeout(Duration.ofSeconds(20), Duration.ofSeconds(60)))
            .isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void otherwiseTheFeedPartGetsHalfTheRowsTimeout() {
        assertThat(SubmissionPublisher.partTimeout(Duration.ofSeconds(60), Duration.ofSeconds(60)))
            .isEqualTo(Duration.ofSeconds(30));
        assertThat(SubmissionPublisher.partTimeout(Duration.ofSeconds(90), Duration.ofSeconds(60)))
            .isEqualTo(Duration.ofSeconds(30));
        assertThat(SubmissionPublisher.partTimeout(Duration.ZERO, Duration.ofSeconds(3)))
            .isEqualTo(Duration.ofMillis(1500));
    }
}
