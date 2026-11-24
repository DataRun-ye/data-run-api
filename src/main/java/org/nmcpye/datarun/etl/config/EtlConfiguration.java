package org.nmcpye.datarun.etl.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EtlProperties.class)
public class EtlConfiguration {
}
