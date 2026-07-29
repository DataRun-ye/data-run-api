package org.nmcpye.datarun.assignmentshadow.replay;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentProjectionReplayCommandTest {

    @Test
    void isolatedRepairClosesContextAfterSuccess() {
        AssignmentProjectionReplay replay = mock(AssignmentProjectionReplay.class);
        ConfigurableApplicationContext context =
            mock(ConfigurableApplicationContext.class);
        when(replay.replay(AssignmentProjectionReplayMode.REPAIR))
            .thenReturn(new AssignmentProjectionReplayReport(3, 2, 1, 0, 0, 2));

        new AssignmentProjectionReplayCommand(
            replay,
            context,
            isolatedEnvironment("repair")
        ).run(mock(ApplicationArguments.class));

        verify(replay).replay(AssignmentProjectionReplayMode.REPAIR);
        verify(context).close();
    }

    @Test
    void validateModeIsExplicit() {
        AssignmentProjectionReplay replay = mock(AssignmentProjectionReplay.class);
        ConfigurableApplicationContext context =
            mock(ConfigurableApplicationContext.class);
        when(replay.replay(AssignmentProjectionReplayMode.VALIDATE))
            .thenReturn(new AssignmentProjectionReplayReport(3, 2, 0, 0, 0, 0));

        new AssignmentProjectionReplayCommand(
            replay,
            context,
            isolatedEnvironment("validate")
        ).run(mock(ApplicationArguments.class));

        verify(replay).replay(AssignmentProjectionReplayMode.VALIDATE);
        verify(context).close();
    }

    @Test
    void commandRefusesRuntimeWithWriters() {
        AssignmentProjectionReplay replay = mock(AssignmentProjectionReplay.class);
        ConfigurableApplicationContext context =
            mock(ConfigurableApplicationContext.class);
        Environment environment = isolatedEnvironment("repair");
        when(environment.getProperty(
            AssignmentProjectionReplayCommand.NO_WRITERS_PROPERTY
        )).thenReturn("false");

        AssignmentProjectionReplayCommand command =
            new AssignmentProjectionReplayCommand(replay, context, environment);

        assertThatThrownBy(
            () -> command.run(mock(ApplicationArguments.class))
        ).isInstanceOf(AssignmentProjectionReplayConflictException.class)
            .hasMessageContaining("no-writers=true");
        verify(replay, never()).replay(AssignmentProjectionReplayMode.REPAIR);
        verify(context).close();
    }

    @Test
    void commandRefusesMissingMode() {
        AssignmentProjectionReplay replay = mock(AssignmentProjectionReplay.class);
        ConfigurableApplicationContext context =
            mock(ConfigurableApplicationContext.class);

        AssignmentProjectionReplayCommand command =
            new AssignmentProjectionReplayCommand(
                replay,
                context,
                isolatedEnvironment(null)
            );

        assertThatThrownBy(
            () -> command.run(mock(ApplicationArguments.class))
        ).isInstanceOf(AssignmentProjectionReplayConflictException.class)
            .hasMessageContaining("mode=validate|repair");
        verify(replay, never()).replay(AssignmentProjectionReplayMode.REPAIR);
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
            AssignmentProjectionReplayCommand.NO_WRITERS_PROPERTY
        )).thenReturn("true");
        when(environment.getProperty(
            AssignmentProjectionReplayCommand.MODE_PROPERTY
        )).thenReturn(mode);
        return environment;
    }
}
