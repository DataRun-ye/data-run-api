package org.nmcpye.datarun.submission.feed;

import org.springframework.core.env.Environment;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Downstream's reader role: the database role Airbyte reads schema {@code feed} as, named by the setting
 * {@value #PROPERTY} ({@code DATARUN_FEED_READER_ROLE} in the environment). {@link FeedReaderGrants} converges the
 * grants on the schema to it at every start.
 *
 * <p>Only a plain role name is used: letters, digits, {@code _} and {@code -}, at most 63 characters. Anything else is
 * treated as no name and logged; the name is still quoted wherever it reaches SQL.</p>
 */
public final class FeedReaderRole {

    /** The setting that names the role. */
    public static final String PROPERTY = "datarun.feed.reader-role";

    private static final Pattern PLAIN_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_-]{0,62}");

    private FeedReaderRole() {
    }

    /** The setting as written, trimmed, or empty when it is not set or blank. */
    public static Optional<String> configured(Environment environment) {
        String value = environment.getProperty(PROPERTY);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    /** Whether a name is used: a plain role name. */
    public static boolean isPlain(String name) {
        return name != null && PLAIN_NAME.matcher(name).matches();
    }
}
