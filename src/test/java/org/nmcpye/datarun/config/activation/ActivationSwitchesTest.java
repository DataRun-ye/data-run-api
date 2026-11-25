package org.nmcpye.datarun.config.activation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Switches are off unless set true; a switch whose requirement is off stays off; settings come from YAML or the
 * environment in any spelling; misconfiguration is logged, never fatal; declaration mistakes fail the start.
 */
class ActivationSwitchesTest {

    private static final ActivationSwitch FIRST = ActivationSwitch.of("first-feature", "turns on the first");
    private static final ActivationSwitch SECOND =
        ActivationSwitch.of("second-feature", "turns on the second", "first-feature");
    private static final ActivationSwitch THIRD =
        ActivationSwitch.of("third-feature", "turns on the third", "second-feature");
    private static final List<ActivationSwitch> ALL = List.of(FIRST, SECOND, THIRD);

    private final Logger logger = (Logger) LoggerFactory.getLogger(ActivationSwitches.class);
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();

    @BeforeEach
    void capture() {
        lines.start();
        logger.addAppender(lines);
    }

    @AfterEach
    void release() {
        logger.detachAppender(lines);
    }

    private static Map<String, String> settings(MockEnvironment environment) {
        return Binder.get(environment).bind(ActivationSwitches.PREFIX, Bindable.mapOf(String.class, String.class))
            .orElse(Map.of());
    }

    @Test
    void everySwitchIsOffUnlessSetTrue() {
        ActivationSwitches switches = new ActivationSwitches(ALL, Map.of());
        assertThat(ALL).noneMatch(switches::isOn);
        assertThat(lines.list).extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("Activation switches: first-feature=off, second-feature=off, third-feature=off");
        assertThatThrownBy(() -> switches.requireMethod(FIRST))
            .isInstanceOfSatisfying(NotEnabledException.class, refused -> {
                assertThat(refused.switchName()).isEqualTo("first-feature");
                assertThat(refused.scope()).isEqualTo(NotEnabledException.Scope.METHOD);
            });
        assertThatThrownBy(() -> switches.requireOperation(FIRST))
            .isInstanceOfSatisfying(NotEnabledException.class,
                refused -> assertThat(refused.scope()).isEqualTo(NotEnabledException.Scope.OPERATION));
    }

    @Test
    void settingsFromYamlOrTheEnvironmentFindTheirSwitchInAnySpelling() {
        MockEnvironment yaml = new MockEnvironment()
            .withProperty("datarun.activation.first-feature", "true")
            .withProperty("datarun.activation.secondFeature", "TRUE");
        ActivationSwitches fromYaml = new ActivationSwitches(ALL, settings(yaml));
        assertThat(fromYaml.isOn(FIRST)).isTrue();
        assertThat(fromYaml.isOn(SECOND)).isTrue();

        // As the process environment: Spring Boot maps DATARUN_X_Y names only in the systemEnvironment source.
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
            new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of(
                "DATARUN_ACTIVATION_FIRST_FEATURE", "true", "DATARUN_ACTIVATION_SECONDFEATURE", "true")));
        Map<String, String> fromVariables = Binder.get(environment)
            .bind(ActivationSwitches.PREFIX, Bindable.mapOf(String.class, String.class)).orElse(Map.of());
        ActivationSwitches fromEnvironment = new ActivationSwitches(ALL, fromVariables);
        assertThat(fromEnvironment.isOn(FIRST)).isTrue();
        assertThat(fromEnvironment.isOn(SECOND)).isTrue();
        assertThat(fromEnvironment.isOn(THIRD)).isFalse();
    }

    @Test
    void aSwitchWhoseRequirementIsOffStaysOffWithAnErrorLine() {
        ActivationSwitches switches = new ActivationSwitches(ALL,
            Map.of("second-feature", "true", "third-feature", "true"));

        assertThat(switches.isOn(SECOND)).isFalse();
        assertThat(switches.isOn(THIRD)).as("its requirement stayed off").isFalse();
        assertThat(lines.list).filteredOn(line -> line.getLevel() == Level.ERROR)
            .extracting(ILoggingEvent::getFormattedMessage)
            .contains("Activation switch second-feature is set on but requires first-feature, which is off; "
                + "second-feature stays off");

        ActivationSwitches inOrder = new ActivationSwitches(ALL,
            Map.of("first-feature", "true", "second-feature", "true", "third-feature", "true"));
        assertThat(ALL).allMatch(inOrder::isOn);
    }

    @Test
    void aBadValueOrAnUnknownSettingIsLoggedAndChangesNothing() {
        ActivationSwitches switches = new ActivationSwitches(ALL,
            Map.of("first-feature", "yes", "from-a-later-release", "true"));

        assertThat(switches.isOn(FIRST)).isFalse();
        assertThat(lines.list).filteredOn(line -> line.getLevel() == Level.WARN)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactlyInAnyOrder(
                "Setting datarun.activation.first-feature is neither true nor false; switch first-feature stays off",
                "Setting datarun.activation.from-a-later-release names no activation switch of this build; ignored");
    }

    @Test
    void withNoSwitchDeclaredThereIsNothingToTurnOn() {
        new ActivationSwitches(List.of(), Map.of("anything", "true"));
        assertThat(lines.list).extracting(ILoggingEvent::getFormattedMessage)
            .contains("Activation switches: none declared");
    }

    @Test
    void declarationMistakesFailTheStart() {
        assertThatThrownBy(() -> new ActivationSwitches(
            List.of(FIRST, ActivationSwitch.of("firstFeature".toLowerCase(), "a twin")), Map.of()))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ActivationSwitches(List.of(SECOND), Map.of()))
            .as("an undeclared requirement").isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ActivationSwitches(List.of(
            ActivationSwitch.of("left", "l", "right"), ActivationSwitch.of("right", "r", "left")), Map.of()))
            .as("a cycle").isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ActivationSwitch.of("Not_Kebab", "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActivationSwitches(List.of(FIRST), Map.of()).isOn(SECOND))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
