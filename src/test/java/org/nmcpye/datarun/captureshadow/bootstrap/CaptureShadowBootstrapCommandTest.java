package org.nmcpye.datarun.captureshadow.bootstrap;

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

class CaptureShadowBootstrapCommandTest {

    @Test
    void isolatedNoWriterCommandClosesContextAfterSuccess() {
        CaptureShadowBootstrap bootstrap = mock(CaptureShadowBootstrap.class);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(bootstrap.run()).thenReturn(emptyReport());

        new CaptureShadowBootstrapCommand(bootstrap, context, isolatedEnvironment())
            .run(mock(ApplicationArguments.class));

        verify(context).close();
    }

    @Test
    void commandRefusesMissingNoWriterAcknowledgement() {
        CaptureShadowBootstrap bootstrap = mock(CaptureShadowBootstrap.class);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        Environment environment = isolatedEnvironment();
        when(environment.getProperty(CaptureShadowBootstrapCommand.NO_WRITERS_PROPERTY))
            .thenReturn("false");

        CaptureShadowBootstrapCommand command = new CaptureShadowBootstrapCommand(
            bootstrap,
            context,
            environment
        );

        assertThatThrownBy(() -> command.run(mock(ApplicationArguments.class)))
            .isInstanceOf(CaptureShadowBootstrapConflictException.class)
            .hasMessageContaining("no-writers=true");
        verify(bootstrap, never()).run();
        verify(context).close();
    }

    @Test
    void commandRefusesWebOrMigrationCapableRuntime() {
        CaptureShadowBootstrap bootstrap = mock(CaptureShadowBootstrap.class);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        Environment environment = isolatedEnvironment();
        when(environment.getProperty("spring.main.web-application-type")).thenReturn("servlet");

        CaptureShadowBootstrapCommand command = new CaptureShadowBootstrapCommand(
            bootstrap,
            context,
            environment
        );

        assertThatThrownBy(() -> command.run(mock(ApplicationArguments.class)))
            .isInstanceOf(CaptureShadowBootstrapConflictException.class)
            .hasMessageContaining("web-application-type=none");
        verify(bootstrap, never()).run();
        verify(context).close();
    }

    private static Environment isolatedEnvironment() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.main.web-application-type")).thenReturn("none");
        when(environment.getProperty("spring.main.lazy-initialization")).thenReturn("true");
        when(environment.getProperty("datarun.scheduling.enabled")).thenReturn("false");
        when(environment.getProperty("spring.liquibase.enabled")).thenReturn("false");
        when(environment.getProperty("application.liquibase.async-start")).thenReturn("false");
        when(environment.getProperty(CaptureShadowBootstrapCommand.NO_WRITERS_PROPERTY))
            .thenReturn("true");
        return environment;
    }

    private static CaptureShadowBootstrapReport emptyReport() {
        CaptureShadowBootstrapReport.ItemCount zero =
            new CaptureShadowBootstrapReport.ItemCount(0, 0);
        return new CaptureShadowBootstrapReport(
            new CaptureSourceBoundary(
                0,
                null,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                null
            ),
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
            0,
            List.of()
        );
    }
}
