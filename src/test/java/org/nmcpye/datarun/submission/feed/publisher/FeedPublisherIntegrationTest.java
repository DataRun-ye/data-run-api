package org.nmcpye.datarun.submission.feed.publisher;

import org.nmcpye.datarun.IntegrationTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The application context the publisher's integration tests share: the switch on, a pool of six connections (writers,
 * publishers and readers run at once), a row timeout of ten seconds with a feed part limit of three, three attempts
 * before a pending change is dead, retries due at once, small batches, the split seam's stub, and a dictionary that is
 * not checked again during a test.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@IntegrationTest
@Import(TestSplitPartners.class)
@TestPropertySource(properties = {
    "datarun.activation.submission-feed=true",
    "spring.datasource.hikari.maximum-pool-size=6",
    "datarun.etl.row-timeout=10s",
    "datarun.feed.part-timeout=3s",
    "datarun.feed.max-attempts=3",
    "datarun.feed.retry-delay=0s",
    "datarun.feed.max-retry-delay=0s",
    "datarun.feed.batch-size=5",
    "datarun.feed.reconcile-scan-size=7",
    "datarun.feed.dictionary-check-delay=PT1H"
})
@interface FeedPublisherIntegrationTest {
}
