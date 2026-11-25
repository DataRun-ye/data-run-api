package org.nmcpye.datarun.submission.feed.publisher;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FeedPublisherProperties.class)
public class FeedPublisherConfiguration {
}
