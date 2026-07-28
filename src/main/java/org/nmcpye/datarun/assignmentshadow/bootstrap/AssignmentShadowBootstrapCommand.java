package org.nmcpye.datarun.assignmentshadow.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = {
        AssignmentShadowBootstrapCommand.ENABLED_PROPERTY,
        AssignmentShadowBootstrapCommand.ISOLATED_PROPERTY
    },
    havingValue = "true"
)
public class AssignmentShadowBootstrapCommand implements ApplicationRunner {

    public static final String ENABLED_PROPERTY = "datarun.assignment-shadow.bootstrap.enabled";
    public static final String ISOLATED_PROPERTY = "datarun.assignment-shadow.bootstrap.isolated";

    private final AssignmentShadowBootstrap bootstrap;
    private final ConfigurableApplicationContext applicationContext;
    private final Environment environment;

    public AssignmentShadowBootstrapCommand(
        AssignmentShadowBootstrap bootstrap,
        ConfigurableApplicationContext applicationContext,
        Environment environment
    ) {
        this.bootstrap = bootstrap;
        this.applicationContext = applicationContext;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            assertIsolatedRuntime();
            AssignmentShadowBootstrapReport report = bootstrap.run();
            System.out.print(report.toOperatorText());
        } catch (AssignmentShadowBootstrapMismatchException exception) {
            System.err.print(exception.report().toOperatorText());
            throw exception;
        } catch (RuntimeException exception) {
            System.err.println("assignment-shadow-bootstrap/v1");
            System.err.println("status=CONFLICT");
            System.err.println("error=" + sanitize(exception.getMessage()));
            throw exception;
        } finally {
            applicationContext.close();
        }
    }

    private void assertIsolatedRuntime() {
        requireProperty("spring.main.web-application-type", "none");
        requireProperty("spring.main.lazy-initialization", "true");
        requireProperty("datarun.scheduling.enabled", "false");
        requireProperty("spring.liquibase.enabled", "false");
        requireProperty("application.liquibase.async-start", "false");
    }

    private void requireProperty(String name, String expected) {
        String actual = environment.getProperty(name);
        if (!expected.equalsIgnoreCase(actual)) {
            throw new AssignmentShadowBootstrapConflictException(
                "Bootstrap requires " + name + "=" + expected
            );
        }
    }

    private static String sanitize(String value) {
        return value == null ? "<no-message>" : value.replaceAll("[\\r\\n]", " ");
    }
}
