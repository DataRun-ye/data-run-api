package org.nmcpye.datarun.config.activation;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A feature that is deployed off and turned on by configuration, so deploying a release and activating what it
 * brings stay separate steps ({@code production-boundaries.md}). A module declares each of its switches as a bean;
 * {@link ActivationSwitches} reads their settings.
 * <p>
 * A switch is on only when {@code datarun.activation.<name>} is {@code true} and every switch it {@link #requires}
 * is on too, which encodes a safe activation order: a feature that is unsafe without another cannot be turned on
 * before it. Names are kebab-case words from the feature's glossary.
 *
 * @param name        the setting's name under {@code datarun.activation}, kebab-case
 * @param description what turning it on does, for the startup log
 * @param requires    the names of the switches that must be on for this one to be on
 */
public record ActivationSwitch(String name, String description, List<String> requires) {

    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9]*(-[a-z0-9]+)*");

    public ActivationSwitch {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("An activation switch is named in kebab-case: " + name);
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("An activation switch says what it turns on");
        }
        requires = List.copyOf(requires);
        if (requires.contains(name)) {
            throw new IllegalArgumentException("An activation switch cannot require itself");
        }
    }

    public static ActivationSwitch of(String name, String description, String... requires) {
        return new ActivationSwitch(name, description, List.of(requires));
    }

    /** The name with case and separators removed: how a setting from YAML or the environment finds its switch. */
    static String normalized(String name) {
        return name.toLowerCase(java.util.Locale.ROOT).replaceAll("[-_.]", "");
    }
}
