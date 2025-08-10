package org.nmcpye.datarun.audit;

import org.nmcpye.datarun.jpa.auditing.service.EntityAuditEventWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestEntityAuditEventWriter {

    @Bean
    EntityAuditEventWriter entityAuditEventWriter() {
        return (target, action) -> {};
    }
}
