package org.nmcpye.datarun.web.v2.fixtures;

import io.swagger.v3.oas.annotations.Hidden;
import org.nmcpye.datarun.config.activation.ActivationSwitch;
import org.nmcpye.datarun.config.activation.ActivationSwitches;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A test-only activation switch, never set in the test configuration, and a v2 action behind it: an operation
 * deployed off. It exists only on the test classpath.
 */
@Configuration
public class V2ActivationFixture {

    public static final ActivationSwitch FIXTURE_ACTION =
        ActivationSwitch.of("fixture-action", "turns on the test-only fixture action");

    @Bean
    public ActivationSwitch fixtureActionSwitch() {
        return FIXTURE_ACTION;
    }

    @Hidden
    @RestController
    public static class SwitchedRoute {

        private final ActivationSwitches switches;

        public SwitchedRoute(ActivationSwitches switches) {
            this.switches = switches;
        }

        @PostMapping("/api/v2/fixtures/switched:run")
        public Map<String, String> run() {
            switches.requireMethod(FIXTURE_ACTION);
            return Map.of("ran", "true");
        }

        /** A method that is on, with one kind of operation in its body behind the switch. */
        @PostMapping("/api/v2/fixtures/switched:apply")
        public Map<String, String> apply(@org.springframework.web.bind.annotation.RequestParam String op) {
            if (op.equals("gated")) {
                switches.requireOperation(FIXTURE_ACTION);
            }
            return Map.of("applied", op);
        }
    }
}
