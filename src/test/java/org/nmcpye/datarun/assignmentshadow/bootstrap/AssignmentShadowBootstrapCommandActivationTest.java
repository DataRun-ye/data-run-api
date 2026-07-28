package org.nmcpye.datarun.assignmentshadow.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AssignmentShadowBootstrapCommandActivationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(AssignmentShadowBootstrap.class, () -> mock(AssignmentShadowBootstrap.class))
        .withUserConfiguration(AssignmentShadowBootstrapCommand.class);

    @Test
    void bootstrapFlagAloneCannotActivateTheCommand() {
        contextRunner
            .withPropertyValues(AssignmentShadowBootstrapCommand.ENABLED_PROPERTY + "=true")
            .run(context -> assertThat(context)
                .doesNotHaveBean(AssignmentShadowBootstrapCommand.class));
    }

    @Test
    void explicitBootstrapAndIsolationFlagsActivateTheCommand() {
        contextRunner
            .withPropertyValues(
                AssignmentShadowBootstrapCommand.ENABLED_PROPERTY + "=true",
                AssignmentShadowBootstrapCommand.ISOLATED_PROPERTY + "=true"
            )
            .run(context -> assertThat(context)
                .hasSingleBean(AssignmentShadowBootstrapCommand.class));
    }
}
