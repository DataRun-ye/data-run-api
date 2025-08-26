package org.nmcpye.datarun.config.datarun;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxConfig {
}
