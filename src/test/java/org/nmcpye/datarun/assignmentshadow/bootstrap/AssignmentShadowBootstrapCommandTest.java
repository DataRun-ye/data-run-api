package org.nmcpye.datarun.assignmentshadow.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentShadowBootstrapCommandTest {

    @Test
    void explicitRunClosesApplicationContextAfterSuccess() {
        AssignmentShadowBootstrap bootstrap = mock(AssignmentShadowBootstrap.class);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        Environment environment = isolatedEnvironment();
        when(bootstrap.run()).thenReturn(emptyReport());

        new AssignmentShadowBootstrapCommand(bootstrap, context, environment)
            .run(mock(ApplicationArguments.class));

        verify(context).close();
    }

    @Test
    void explicitRunClosesApplicationContextAfterConflict() {
        AssignmentShadowBootstrap bootstrap = mock(AssignmentShadowBootstrap.class);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        Environment environment = isolatedEnvironment();
        AssignmentShadowBootstrapConflictException conflict =
            new AssignmentShadowBootstrapConflictException("test conflict");
        when(bootstrap.run()).thenThrow(conflict);

        AssignmentShadowBootstrapCommand command =
            new AssignmentShadowBootstrapCommand(bootstrap, context, environment);

        assertThatThrownBy(() -> command.run(mock(ApplicationArguments.class)))
            .isSameAs(conflict);
        verify(context).close();
    }

    @Test
    void explicitRunRefusesAContextThatCanRunLiquibase() {
        AssignmentShadowBootstrap bootstrap = mock(AssignmentShadowBootstrap.class);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        Environment environment = isolatedEnvironment();
        when(environment.getProperty("spring.liquibase.enabled")).thenReturn("true");

        AssignmentShadowBootstrapCommand command =
            new AssignmentShadowBootstrapCommand(bootstrap, context, environment);

        assertThatThrownBy(() -> command.run(mock(ApplicationArguments.class)))
            .isInstanceOf(AssignmentShadowBootstrapConflictException.class)
            .hasMessageContaining("spring.liquibase.enabled=false");
        verify(bootstrap, never()).run();
        verify(context).close();
    }

    private static Environment isolatedEnvironment() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.main.web-application-type")).thenReturn("none");
        when(environment.getProperty("datarun.scheduling.enabled")).thenReturn("false");
        when(environment.getProperty("spring.liquibase.enabled")).thenReturn("false");
        when(environment.getProperty("application.liquibase.async-start")).thenReturn("false");
        return environment;
    }

    private static AssignmentShadowBootstrapReport emptyReport() {
        AssignmentShadowBootstrapReport.ItemCount zero =
            new AssignmentShadowBootstrapReport.ItemCount(0, 0);
        return new AssignmentShadowBootstrapReport(
            0,
            0,
            0,
            0,
            List.of(),
            zero,
            zero,
            zero,
            zero,
            zero,
            zero,
            zero,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0
        );
    }
}
