package org.nmcpye.datarun.submission.feed.publisher;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The split seam's test stub: no change splits a submission yet, so a test names the pairs a split would record, and
 * the publisher writes both heads together. Empty unless a test fills it; each test empties it after itself.
 */
@TestConfiguration
class TestSplitPartners {

    static final Map<String, List<String>> PARTNERS = new ConcurrentHashMap<>();

    @Bean
    SplitPartners splitPartners() {
        return (submissionId, version) -> PARTNERS.getOrDefault(submissionId, List.of());
    }
}
