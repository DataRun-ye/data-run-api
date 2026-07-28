package org.nmcpye.datarun.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigurationSchedulingTest {

    private static final String SCHEDULED_PROCESSOR_BEAN =
        "org.springframework.context.annotation.internalScheduledAnnotationProcessor";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(AsyncConfiguration.SchedulingConfiguration.class);

    @Test
    void schedulingRemainsEnabledWhenPropertyIsAbsent() {
        contextRunner.run(context -> assertThat(context.containsBean(SCHEDULED_PROCESSOR_BEAN)).isTrue());
    }

    @Test
    void schedulingCanBeDisabledForBootstrapProcess() {
        contextRunner
            .withPropertyValues("datarun.scheduling.enabled=false")
            .run(context -> assertThat(context.containsBean(SCHEDULED_PROCESSOR_BEAN)).isFalse());
    }

    @Test
    void schedulingRemainsDisabledForExistingTestProductionProfile() {
        contextRunner
            .withInitializer(context -> context.getEnvironment().setActiveProfiles("testprod"))
            .run(context -> assertThat(context.containsBean(SCHEDULED_PROCESSOR_BEAN)).isFalse());
    }
}
