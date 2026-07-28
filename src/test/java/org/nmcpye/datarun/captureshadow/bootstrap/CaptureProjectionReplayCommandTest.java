package org.nmcpye.datarun.captureshadow.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaptureProjectionReplayCommandTest {

    @Test
    void isolatedRepairClosesContextAfterSuccess() {
        CaptureProjectionReplay replay = mock(CaptureProjectionReplay.class);
        ConfigurableApplicationContext context =
            mock(ConfigurableApplicationContext.class);
        when(replay.replay(CaptureReplayMode.REPAIR))
            .thenReturn(new CaptureReplayReport(2, 3, 1));

        new CaptureProjectionReplayCommand(
            replay,
            context,
            isolatedEnvironment("repair")
        ).run(mock(ApplicationArguments.class));

        verify(context).close();
    }

    @Test
    void validateModeIsPassedExplicitly() {
        CaptureProjectionReplay replay = mock(CaptureProjectionReplay.class);
        ConfigurableApplicationContext context =
            mock(ConfigurableApplicationContext.class);
        when(replay.replay(CaptureReplayMode.VALIDATE))
            .thenReturn(new CaptureReplayReport(2, 3, 0));

        new CaptureProjectionReplayCommand(
            replay,
            context,
            isolatedEnvironment("validate")
        ).run(mock(ApplicationArguments.class));

        verify(replay).replay(CaptureReplayMode.VALIDATE);
        verify(context).close();
    }

    @Test
    void commandRefusesRuntimeWithWriters() {
        CaptureProjectionReplay replay = mock(CaptureProjectionReplay.class);
        ConfigurableApplicationContext context =
            mock(ConfigurableApplicationContext.class);
        Environment environment = isolatedEnvironment("repair");
        when(environment.getProperty(
            CaptureProjectionReplayCommand.NO_WRITERS_PROPERTY
        )).thenReturn("false");

        CaptureProjectionReplayCommand command =
            new CaptureProjectionReplayCommand(
                replay,
                context,
                environment
            );

        assertThatThrownBy(
            () -> command.run(mock(ApplicationArguments.class))
        ).isInstanceOf(CaptureReplayConflictException.class)
            .hasMessageContaining("no-writers=true");
        verify(replay, never()).replay(CaptureReplayMode.REPAIR);
        verify(context).close();
    }

    @Test
    void commandRefusesMissingMode() {
        CaptureProjectionReplay replay = mock(CaptureProjectionReplay.class);
        ConfigurableApplicationContext context =
            mock(ConfigurableApplicationContext.class);
        Environment environment = isolatedEnvironment(null);

        CaptureProjectionReplayCommand command =
            new CaptureProjectionReplayCommand(
                replay,
                context,
                environment
            );

        assertThatThrownBy(
            () -> command.run(mock(ApplicationArguments.class))
        ).isInstanceOf(CaptureReplayConflictException.class)
            .hasMessageContaining("mode=validate|repair");
        verify(replay, never()).replay(CaptureReplayMode.REPAIR);
        verify(context).close();
    }

    private static Environment isolatedEnvironment(String mode) {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.main.web-application-type"))
            .thenReturn("none");
        when(environment.getProperty("spring.main.lazy-initialization"))
            .thenReturn("true");
        when(environment.getProperty("datarun.scheduling.enabled"))
            .thenReturn("false");
        when(environment.getProperty("spring.liquibase.enabled"))
            .thenReturn("false");
        when(environment.getProperty("application.liquibase.async-start"))
            .thenReturn("false");
        when(environment.getProperty(
            CaptureProjectionReplayCommand.NO_WRITERS_PROPERTY
        )).thenReturn("true");
        when(environment.getProperty(
            CaptureProjectionReplayCommand.MODE_PROPERTY
        )).thenReturn(mode);
        return environment;
    }
}
