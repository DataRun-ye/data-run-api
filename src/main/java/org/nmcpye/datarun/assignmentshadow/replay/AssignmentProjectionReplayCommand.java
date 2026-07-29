package org.nmcpye.datarun.assignmentshadow.replay;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@ConditionalOnProperty(
    name = {
        AssignmentProjectionReplayCommand.ENABLED_PROPERTY,
        AssignmentProjectionReplayCommand.ISOLATED_PROPERTY,
        AssignmentProjectionReplayCommand.NO_WRITERS_PROPERTY
    },
    havingValue = "true"
)
public class AssignmentProjectionReplayCommand implements ApplicationRunner {

    public static final String ENABLED_PROPERTY =
        "datarun.assignment-shadow.replay.enabled";
    public static final String ISOLATED_PROPERTY =
        "datarun.assignment-shadow.replay.isolated";
    public static final String NO_WRITERS_PROPERTY =
        "datarun.assignment-shadow.replay.no-writers";
    public static final String MODE_PROPERTY =
        "datarun.assignment-shadow.replay.mode";

    private final AssignmentProjectionReplay replay;
    private final ConfigurableApplicationContext applicationContext;
    private final Environment environment;

    public AssignmentProjectionReplayCommand(
        AssignmentProjectionReplay replay,
        ConfigurableApplicationContext applicationContext,
        Environment environment
    ) {
        this.replay = replay;
        this.applicationContext = applicationContext;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            assertIsolatedRuntime();
            System.out.print(replay.replay(mode()).toOperatorText());
        } catch (RuntimeException exception) {
            System.err.println("assignment-projection-replay/v1");
            System.err.println("status=CONFLICT");
            System.err.println("error=" + sanitize(exception.getMessage()));
            throw exception;
        } finally {
            applicationContext.close();
        }
    }

    private AssignmentProjectionReplayMode mode() {
        String value = environment.getProperty(MODE_PROPERTY);
        try {
            return AssignmentProjectionReplayMode.valueOf(
                value == null ? "" : value.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new AssignmentProjectionReplayConflictException(
                "Replay requires " + MODE_PROPERTY + "=validate|repair",
                exception
            );
        }
    }

    private void assertIsolatedRuntime() {
        requireProperty("spring.main.web-application-type", "none");
        requireProperty("spring.main.lazy-initialization", "true");
        requireProperty("datarun.scheduling.enabled", "false");
        requireProperty("spring.liquibase.enabled", "false");
        requireProperty("application.liquibase.async-start", "false");
        requireProperty(NO_WRITERS_PROPERTY, "true");
    }

    private void requireProperty(String name, String expected) {
        String actual = environment.getProperty(name);
        if (!expected.equalsIgnoreCase(actual)) {
            throw new AssignmentProjectionReplayConflictException(
                "Replay requires " + name + "=" + expected
            );
        }
    }

    private static String sanitize(String value) {
        return value == null
            ? "<no-message>"
            : value.replaceAll("[\\r\\n]", " ");
    }
}
