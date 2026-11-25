package org.nmcpye.datarun.submission.feed;

import org.springframework.core.env.Environment;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Downstream's reading role: the database role Airbyte reads schema {@code feed} as. Liquibase changeset
 * {@code 20261125-d02-feed-reader-grant} grants it USAGE on the schema, SELECT on its tables and default privileges at
 * the first start after it is named here, if it exists then.
 *
 * <p>It is the setting {@value #PROPERTY} ({@code DATARUN_FEED_READER_ROLE} in the environment), passed to Liquibase as
 * the parameter {@value #LIQUIBASE_PARAMETER}. Only a plain role name is passed: letters, digits, {@code _} and
 * {@code -}, at most 63 characters, so the changeset can quote it as is. Anything else is passed as no name, and
 * {@link FeedReaderGrantCheck} says so at startup.</p>
 */
public final class FeedReaderRole {

    /** The setting that names the role. */
    public static final String PROPERTY = "datarun.feed.reader-role";

    /** The Liquibase parameter the grant's changeset reads; empty when no usable name is set. */
    public static final String LIQUIBASE_PARAMETER = "feedReaderRole";

    private static final Pattern PLAIN_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_-]{0,62}");

    private FeedReaderRole() {
    }

    /** The setting as written, trimmed, or empty when it is not set or blank. */
    public static Optional<String> configured(Environment environment) {
        String value = environment.getProperty(PROPERTY);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    /** Whether a name can be passed to the changeset: a plain role name. */
    public static boolean isPlain(String name) {
        return name != null && PLAIN_NAME.matcher(name).matches();
    }

    /** The value of {@value #LIQUIBASE_PARAMETER}: the configured name when it is plain, otherwise empty. */
    public static String liquibaseParameter(Environment environment) {
        return configured(environment).filter(FeedReaderRole::isPlain).orElse("");
    }
}
