package org.nmcpye.datarun.jpa.datasubmission.command;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Collects what one class logs while a test runs, to check that log lines carry no login or submitted value. */
final class LogCapture implements AutoCloseable {

    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    LogCapture(Class<?> type) {
        logger = (Logger) LoggerFactory.getLogger(type);
        appender.start();
        logger.addAppender(appender);
    }

    List<ILoggingEvent> events() {
        return appender.list;
    }

    List<String> lines() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    /** No line names any of {@code secrets}, and no exception (whose message could) reaches the log. */
    void assertNothingLeaks(String... secrets) {
        assertThat(appender.list).as("log events").isNotEmpty();
        for (ILoggingEvent event : appender.list) {
            assertThat(event.getThrowableProxy())
                .as("an exception logged with %s, whose message can quote values", event.getFormattedMessage())
                .isNull();
            for (String secret : secrets) {
                assertThat(event.getFormattedMessage()).doesNotContain(secret);
            }
        }
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
    }
}
