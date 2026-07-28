package org.nmcpye.datarun.assignmentshadow;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class AssignmentAuthorityConfiguration {

    @Bean
    Clock assignmentAuthorityClock() {
        return Clock.systemUTC();
    }
}
