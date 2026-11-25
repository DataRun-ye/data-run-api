package org.nmcpye.datarun.submission.feed;

import org.nmcpye.datarun.config.activation.ActivationSwitch;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The submission feed's activation switch, off by default ({@code docs/agent-context/api-v2.md}, Activation Switches):
 * {@code datarun.activation.submission-feed}, {@code DATARUN_ACTIVATION_SUBMISSION_FEED=true} in production. While it
 * is off the publisher writes nothing to schema {@code feed}; turning it on, and restarting, starts publication, and
 * setting it back to false is the rollback (contract, Rollout).
 */
@Configuration
public class FeedActivation {

    public static final ActivationSwitch SUBMISSION_FEED = ActivationSwitch.of("submission-feed",
        "the submission feed: its publisher writes schema feed");

    @Bean
    public ActivationSwitch submissionFeedSwitch() {
        return SUBMISSION_FEED;
    }
}
