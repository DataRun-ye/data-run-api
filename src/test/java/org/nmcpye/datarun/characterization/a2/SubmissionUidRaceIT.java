package org.nmcpye.datarun.characterization.a2;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.zaxxer.hikari.HikariDataSource;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionUploads;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The expected duplicate-uid race, for real (6.5.5). A second save of the same new submission holds its insert
 * uncommitted while the phone's upload of that uid runs: neither sees the other's row, the upload's insert waits on
 * the uid's unique index, and fails when the other save commits. That is how a phone's re-send during its first
 * request, or a held release meeting a live copy, ends. Not a golden: it reuses the A2 seed, and pins the logs.
 * <ul>
 *   <li>the phone's answer is the one it always got: the submission failed, send it again;</li>
 *   <li>the upload logs one WARN line with the ids and SQLState 23505, and no ERROR;</li>
 *   <li>Hibernate's own ERROR line for the insert stays in the log, and the reporting filter drops it.</li>
 * </ul>
 */
class SubmissionUidRaceIT extends A2GoldenSupport {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SentryOptions.BeforeSendCallback errorReportingWithoutValues;

    @Test
    void aSecondSaveOfTheSameNewUidIsAnExpectedRace() throws Exception {
        String uid = uid("race");
        Logger uploads = (Logger) LoggerFactory.getLogger(SubmissionUploads.class);
        Logger hibernate = (Logger) LoggerFactory.getLogger("org.hibernate.engine.jdbc.spi.SqlExceptionHelper");
        ListAppender<ILoggingEvent> uploadLines = capture(uploads);
        ListAppender<ILoggingEvent> hibernateLines = capture(hibernate);
        MockHttpServletResponse response;
        // Connections of their own: the test profiles' pool holds one, which the upload takes.
        HikariDataSource pool = dataSource.unwrap(HikariDataSource.class);
        try (Connection other = connect(pool); Connection watcher = connect(pool)) {
            other.setAutoCommit(false);
            insertUncommitted(other, uid);

            CompletableFuture<MockHttpServletResponse> upload = CompletableFuture.supplyAsync(() -> upload(uid));
            awaitTheUploadWaitingOnTheUid(watcher);
            other.commit();
            response = upload.get(10, TimeUnit.SECONDS);
        } finally {
            uploads.detachAppender(uploadLines);
            hibernate.detachAppender(hibernateLines);
        }

        assertThat(response.getStatus()).isEqualTo(200);
        JsonNode answer = json.readTree(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(answer.path("failed").path(uid).asText()).isEqualTo(SubmissionUploads.UNEXPECTED_FAILURE);
        assertThat(answer.path("created")).isEmpty();

        assertThat(uploadLines.list).filteredOn(line -> line.getLevel() == Level.ERROR).isEmpty();
        assertThat(uploadLines.list).singleElement().satisfies(line -> {
            assertThat(line.getLevel()).isEqualTo(Level.WARN);
            assertThat(line.getFormattedMessage())
                .startsWith("Submission " + uid + " (form " + FORM_A + ") from user ")
                .contains("not saved: another save of the same uid came first: ")
                .contains("SQLState 23505");
        });

        List<ILoggingEvent> hibernateErrors = hibernateLines.list.stream()
            .filter(line -> line.getLevel() == Level.ERROR).toList();
        assertThat(hibernateErrors).as("Hibernate's line stays in the container log").isNotEmpty()
            .allSatisfy(line -> {
                assertThat(line.getFormattedMessage()).contains("unique constraint \"uk_data_submission_uid\"");
                assertThat(errorReportingWithoutValues.execute(asSentryEvent(line), new Hint()))
                    .as("reported to GlitchTip: %s", line.getFormattedMessage()).isNull();
            });
    }

    private MockHttpServletResponse upload(String uid) {
        try {
            String body = json.writeValueAsString(A2Payloads.body(item(G3, uid, FORM_A, version("FORM_A.v1"),
                ASSIGN_T1_V1, ordinary(G3, uid, "first", "r1", "s1", 1))));
            return mvc.perform(post(A2Payloads.BULK_VERSIONED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + A2Tokens.token(jwtKey, FIELD_1))
                    .header("X-App-Version", "6.1.2+57")
                    .content(body))
                .andReturn().getResponse();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Connection connect(HikariDataSource pool) throws Exception {
        return DriverManager.getConnection(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword());
    }

    /** The other save's row: only what the table requires. */
    private static void insertUncommitted(Connection connection, String uid) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement("""
            INSERT INTO data_submission (id, uid, assignment_uid, activity_uid, team_uid, org_unit_uid, created_by,
                                         created_date)
            VALUES ('01HA2RACE0000000000000000X', ?, ?, 'a2-race', 'a2-race', 'a2-race', 'a2-race', ?)""")) {
            insert.setString(1, uid);
            insert.setString(2, ASSIGN_T1_V1);
            insert.setTimestamp(3, Timestamp.from(Instant.now()));
            insert.executeUpdate();
        }
    }

    /** Until a session of this database waits on a lock: the upload's insert, behind the other save's row. */
    private static void awaitTheUploadWaitingOnTheUid(Connection watcher) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        try (PreparedStatement waiting = watcher.prepareStatement("""
            SELECT count(*) FROM pg_stat_activity
            WHERE datname = current_database() AND state = 'active' AND wait_event_type = 'Lock'""")) {
            while (System.nanoTime() < deadline) {
                try (ResultSet count = waiting.executeQuery()) {
                    if (count.next() && count.getInt(1) > 0) {
                        return;
                    }
                }
                Thread.sleep(50);
            }
        }
        throw new AssertionError("the upload never waited on the other save's uid");
    }

    private static ListAppender<ILoggingEvent> capture(Logger logger) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    /** The event the logback appender of the Sentry SDK makes of a line. */
    private static SentryEvent asSentryEvent(ILoggingEvent line) {
        SentryEvent event = new SentryEvent();
        event.setLogger(line.getLoggerName());
        Message message = new Message();
        message.setMessage(line.getMessage());
        message.setFormatted(line.getFormattedMessage());
        event.setMessage(message);
        return event;
    }
}
