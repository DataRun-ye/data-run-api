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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Downstream's reader role comes from one setting, from YAML or the environment, and only a plain role name is used;
 * with no role named, or a name that is not plain, the start logs one WARN and reaches no database.
 */
class FeedReaderRoleTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(FeedReaderGrants.class);
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
        assertThat(FeedReaderRole.configured(fromYaml)).contains("Airbyte-Reader");
    }

    @Test
    void onlyAPlainRoleNameIsUsed() {
        assertThat(FeedReaderRole.isPlain("airbyte")).isTrue();
        assertThat(FeedReaderRole.isPlain("_reader-2")).isTrue();
        assertThat(FeedReaderRole.isPlain("a".repeat(63))).isTrue();
        assertThat(FeedReaderRole.isPlain("a".repeat(64))).as("longer than PostgreSQL keeps").isFalse();
        assertThat(FeedReaderRole.isPlain("2reader")).isFalse();
        assertThat(FeedReaderRole.isPlain("air\"byte")).isFalse();
        assertThat(FeedReaderRole.isPlain("air'byte")).isFalse();
        assertThat(FeedReaderRole.isPlain("air byte")).isFalse();
        assertThat(FeedReaderRole.isPlain("air%byte")).isFalse();
        assertThat(FeedReaderRole.isPlain(null)).isFalse();
    }

    @Test
    void noRoleNamed_orANameThatIsNotPlain_isOneWarnAndNoDatabaseWork() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);

        new FeedReaderGrants(jdbc, transactions, new MockEnvironment()).convergeAtStart();
        new FeedReaderGrants(jdbc, transactions,
            new MockEnvironment().withProperty(FeedReaderRole.PROPERTY, "x\"; DROP SCHEMA feed; --")).convergeAtStart();

        verifyNoInteractions(jdbc, transactions);
        assertThat(lines.list).extracting(line -> line.getLevel() + " " + line.getFormattedMessage()).containsExactly(
            "WARN Downstream's grant on schema feed is not made: no reader role is set (datarun.feed.reader-role); "
                + "grants on feed are left as they are",
            "WARN Downstream's grant on schema feed is not made: datarun.feed.reader-role is not a plain role name "
                + "(letters, digits, _ and -, at most 63 characters); grants on feed are left as they are");
    }
}
