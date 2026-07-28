package org.nmcpye.datarun.jpa.auditing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.auditing.repository.EntityAuditEventRepository;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.convert.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AsyncEntityAuditEventWriterTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(EntityAuditEventRepository.class, () -> mock(EntityAuditEventRepository.class))
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withBean(AsyncEntityAuditEventWriter.class);

    @Test
    void startsWithoutWebConversionInfrastructure() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AsyncEntityAuditEventWriter.class);
            assertThat(context).doesNotHaveBean(ConversionService.class);
        });
    }
}
