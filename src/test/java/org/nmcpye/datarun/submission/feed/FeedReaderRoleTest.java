package org.nmcpye.datarun.submission.feed;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Downstream's reading role comes from one setting, reaches Liquibase only as a plain role name, and every start logs
 * exactly one line on the grant: a WARN naming what is missing, or an INFO when the role can read schema feed.
 */
class FeedReaderRoleTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(FeedReaderGrantCheck.class);
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

    private java.util.List<String> logged() {
        return lines.list.stream().map(line -> line.getLevel() + " " + line.getFormattedMessage()).toList();
    }

    @Test
    void theSettingIsReadFromYamlOrTheEnvironmentAndTrimmed() {
        assertThat(FeedReaderRole.configured(new MockEnvironment())).isEmpty();
        assertThat(FeedReaderRole.configured(new MockEnvironment().withProperty(FeedReaderRole.PROPERTY, "  ")))
            .isEmpty();
        assertThat(FeedReaderRole.configured(new MockEnvironment().withProperty(FeedReaderRole.PROPERTY, " airbyte ")))
            .contains("airbyte");

        StandardEnvironment fromHost = new StandardEnvironment();
        fromHost.getPropertySources().addFirst(new SystemEnvironmentPropertySource("host",
            Map.of("DATARUN_FEED_READER_ROLE", "airbyte_reader")));
        assertThat(FeedReaderRole.configured(fromHost)).contains("airbyte_reader");

        StandardEnvironment fromYaml = new StandardEnvironment();
        fromYaml.getPropertySources().addFirst(new MapPropertySource("yaml",
            Map.of("datarun.feed.reader-role", "Airbyte-Reader")));
        assertThat(FeedReaderRole.liquibaseParameter(fromYaml)).isEqualTo("Airbyte-Reader");
    }

    @Test
    void onlyAPlainRoleNameReachesLiquibase() {
        assertThat(FeedReaderRole.isPlain("airbyte")).isTrue();
        assertThat(FeedReaderRole.isPlain("_reader-2")).isTrue();
        assertThat(FeedReaderRole.isPlain("a".repeat(63))).isTrue();
        assertThat(FeedReaderRole.isPlain("a".repeat(64))).as("longer than PostgreSQL keeps").isFalse();
        assertThat(FeedReaderRole.isPlain("2reader")).isFalse();
        assertThat(FeedReaderRole.isPlain("air\"byte")).isFalse();
        assertThat(FeedReaderRole.isPlain("air'byte")).isFalse();
        assertThat(FeedReaderRole.isPlain("air byte")).isFalse();
        assertThat(FeedReaderRole.isPlain("${feedReaderRole}")).isFalse();
        assertThat(FeedReaderRole.isPlain(null)).isFalse();

        assertThat(FeedReaderRole.liquibaseParameter(new MockEnvironment())).isEmpty();
        assertThat(FeedReaderRole.liquibaseParameter(new MockEnvironment()
            .withProperty(FeedReaderRole.PROPERTY, "x\"; DROP SCHEMA feed; --"))).isEmpty();
    }

    @Test
    void noRoleSetIsOneWarnNamingTheMissingGrant() {
        FeedReaderGrantCheck.check(Optional.empty(), role -> {
            throw new AssertionError("nothing to read");
        });
        assertThat(logged()).containsExactly(
            "WARN Downstream's grant on schema feed is not made: no reader role is set (datarun.feed.reader-role)");
    }

    @Test
    void eachStateOfTheCatalogIsOneLine() {
        FeedReaderGrantCheck.check(Optional.of("airbyte"), role -> new FeedReaderGrantCheck.State(true, true, true, true));
        FeedReaderGrantCheck.check(Optional.of("airbyte"), role -> new FeedReaderGrantCheck.State(false, true, false, false));
        FeedReaderGrantCheck.check(Optional.of("airbyte"), role -> new FeedReaderGrantCheck.State(true, true, false, false));
        FeedReaderGrantCheck.check(Optional.of("airbyte"), role -> new FeedReaderGrantCheck.State(true, true, false, true));
        FeedReaderGrantCheck.check(Optional.of("airbyte"), role -> new FeedReaderGrantCheck.State(true, false, false, false));
        FeedReaderGrantCheck.check(Optional.of("airbyte"), role -> {
            throw new QueryTimeoutException("a value must never reach the log");
        });

        assertThat(logged()).containsExactly(
            "INFO Schema feed is readable by reader role airbyte",
            "WARN Downstream's grant on schema feed is not made: reader role airbyte does not exist; the first start "
                + "after it is created makes it",
            "WARN Downstream's grant on schema feed is not made yet for reader role airbyte, created after this start's "
                + "migrations: the next start makes it",
            "WARN Reader role airbyte cannot read schema feed, and changeset 20261125-d02-feed-reader-grant already ran "
                + "(for this role or another): grant it USAGE on feed, SELECT on its tables and default privileges by hand",
            "WARN Downstream's grant on schema feed is not made: schema feed does not exist",
            "WARN Could not check downstream's grant on schema feed for reader role airbyte: QueryTimeoutException");
    }
}
