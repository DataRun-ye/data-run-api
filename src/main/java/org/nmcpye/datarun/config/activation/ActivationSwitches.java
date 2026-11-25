package org.nmcpye.datarun.config.activation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

/**
 * The state of every declared {@link ActivationSwitch}, read once at startup from {@code datarun.activation.*}.
 * <p>
 * Every switch is off unless its setting is {@code true}; nothing is on by default, in production or elsewhere. A
 * setting may be written as YAML ({@code datarun.activation.some-feature: true}) or as an environment variable
 * ({@code DATARUN_ACTIVATION_SOME_FEATURE=true}); case and separators do not matter. Turning a switch on or off
 * means changing the setting and restarting, which is also its rollback.
 * <p>
 * Misconfiguration never stops the application, so phones keep their service:
 * <ul>
 *   <li>a switch set on while a switch it requires is off stays off, with an ERROR line (reported);</li>
 *   <li>a value other than true or false leaves the switch off, with a WARN line;</li>
 *   <li>a setting that names no switch of this build is ignored with a WARN line: a later release's setting must
 *       not stop a rollback to this one.</li>
 * </ul>
 * Declaring two switches with one name, or requiring an undeclared switch or in a cycle, is a programming error and
 * fails the start. The states are logged once at startup.
 */
@Component
public class ActivationSwitches {

    static final String PREFIX = "datarun.activation";

    private static final Logger log = LoggerFactory.getLogger(ActivationSwitches.class);

    private final Map<String, Boolean> states;

    @Autowired
    public ActivationSwitches(ObjectProvider<ActivationSwitch> declared, Environment environment) {
        this(declared.orderedStream().toList(),
            Binder.get(environment).bind(PREFIX, Bindable.mapOf(String.class, String.class)).orElse(Map.of()));
    }

    /** The switches and their settings as given; the application uses the constructor above. */
    ActivationSwitches(List<ActivationSwitch> declared, Map<String, String> settings) {
        Map<String, ActivationSwitch> byName = new LinkedHashMap<>();
        Map<String, String> byNormalized = new HashMap<>();
        for (ActivationSwitch candidate : declared) {
            String previous = byNormalized.put(ActivationSwitch.normalized(candidate.name()), candidate.name());
            if (previous != null) {
                throw new IllegalStateException("Two activation switches are named " + candidate.name());
            }
            byName.put(candidate.name(), candidate);
        }
        for (ActivationSwitch candidate : declared) {
            for (String required : candidate.requires()) {
                if (!byName.containsKey(required)) {
                    throw new IllegalStateException(
                        "Activation switch " + candidate.name() + " requires " + required + ", which is not declared");
                }
            }
            requireNoCycle(candidate, byName, new java.util.ArrayDeque<>());
        }

        Map<String, Boolean> configured = new LinkedHashMap<>();
        settings.forEach((key, value) -> {
            String name = byNormalized.get(ActivationSwitch.normalized(key));
            if (name == null) {
                log.warn("Setting {}.{} names no activation switch of this build; ignored", PREFIX, key);
                return;
            }
            String text = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            if (text.equals("true") || text.equals("false")) {
                configured.put(name, text.equals("true"));
            } else {
                log.warn("Setting {}.{} is neither true nor false; switch {} stays off", PREFIX, key, name);
            }
        });

        Map<String, Boolean> resolved = new LinkedHashMap<>();
        for (ActivationSwitch candidate : declared) {
            resolve(candidate, byName, configured, resolved);
        }
        this.states = Map.copyOf(resolved);
        logStates(declared);
    }

    private static void requireNoCycle(ActivationSwitch candidate, Map<String, ActivationSwitch> byName,
                                       java.util.Deque<String> path) {
        if (path.contains(candidate.name())) {
            throw new IllegalStateException("Activation switches require each other in a cycle: " + path);
        }
        path.push(candidate.name());
        for (String required : candidate.requires()) {
            requireNoCycle(byName.get(required), byName, path);
        }
        path.pop();
    }

    private static boolean resolve(ActivationSwitch candidate, Map<String, ActivationSwitch> byName,
                                   Map<String, Boolean> configured, Map<String, Boolean> resolved) {
        Boolean known = resolved.get(candidate.name());
        if (known != null) {
            return known;
        }
        boolean on = configured.getOrDefault(candidate.name(), false);
        if (on) {
            for (String required : candidate.requires()) {
                if (!resolve(byName.get(required), byName, configured, resolved)) {
                    log.error("Activation switch {} is set on but requires {}, which is off; {} stays off",
                        candidate.name(), required, candidate.name());
                    on = false;
                    break;
                }
            }
        }
        resolved.put(candidate.name(), on);
        return on;
    }

    private void logStates(List<ActivationSwitch> declared) {
        if (declared.isEmpty()) {
            log.info("Activation switches: none declared");
            return;
        }
        StringJoiner line = new StringJoiner(", ");
        declared.forEach(candidate -> line.add(candidate.name() + "=" + (states.get(candidate.name()) ? "on" : "off")));
        log.info("Activation switches: {}", line);
    }

    public boolean isOn(ActivationSwitch candidate) {
        Boolean on = states.get(candidate.name());
        if (on == null) {
            throw new IllegalArgumentException("Activation switch " + candidate.name() + " is not declared");
        }
        return on;
    }

    /**
     * For a whole method behind the switch.
     *
     * @throws NotEnabledException when the switch is off; a v2 request answers it 422 {@code method-not-enabled}
     */
    public void requireMethod(ActivationSwitch candidate) {
        require(candidate, NotEnabledException.Scope.METHOD);
    }

    /**
     * For one kind of operation within a request (an edit's operation list), behind the switch.
     *
     * @throws NotEnabledException when the switch is off; a v2 request answers it 422 {@code operation-not-enabled}
     */
    public void requireOperation(ActivationSwitch candidate) {
        require(candidate, NotEnabledException.Scope.OPERATION);
    }

    private void require(ActivationSwitch candidate, NotEnabledException.Scope scope) {
        if (!isOn(candidate)) {
            throw new NotEnabledException(candidate.name(), scope);
        }
    }
}
