package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.TransactionTimedOutException;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/** The reason codes a pending change keeps: codes only, never a failure's text. */
class FeedFailureTest {

    @Test
    void aPublisherFailureKeepsItsOwnCode() {
        assertThat(FeedFailure.reasonOf(new FeedFailure(FeedFailure.PLACEMENT_UNRESOLVED)))
            .isEqualTo("PLACEMENT_UNRESOLVED");
        assertThat(FeedFailure.reasonOf(new IllegalStateException("wrapped", new FeedFailure(FeedFailure.HEAD_MISSING))))
            .isEqualTo("HEAD_MISSING");
    }

    @Test
    void everyKindOfTimeoutIsATimeout() {
        assertThat(FeedFailure.reasonOf(new DataAccessResourceFailureException("cancelled",
            new SQLException("canceling statement due to user request", "57014")))).isEqualTo("TIMEOUT");
        assertThat(FeedFailure.reasonOf(new QueryTimeoutException("x"))).isEqualTo("TIMEOUT");
        assertThat(FeedFailure.reasonOf(new TransactionTimedOutException("x"))).isEqualTo("TIMEOUT");
        assertThat(FeedFailure.reasonOf(new SQLTimeoutException("the feed part's time is up", "57014")))
            .isEqualTo("TIMEOUT");
    }

    @Test
    void anyOtherDatabaseErrorIsItsSqlState() {
        assertThat(FeedFailure.reasonOf(new DataIntegrityViolationException("x",
            new SQLException("null value in column", "23502")))).isEqualTo("DATABASE_23502");
        BatchUpdateException batch = new BatchUpdateException("batch with values", "22000", new int[0]);
        batch.setNextException(new SQLException("the cause", "22P02"));
        assertThat(FeedFailure.reasonOf(new DataIntegrityViolationException("x", batch)))
            .as("a batch error's own state, not the batch's").isEqualTo("DATABASE_22P02");
    }

    @Test
    void anythingElseIsUnexpectedAndTheLogLineNamesOnlyTypes() {
        IllegalStateException failure = new IllegalStateException("a message that could quote a value",
            new NumberFormatException("12345"));
        assertThat(FeedFailure.reasonOf(failure)).isEqualTo("UNEXPECTED");
        assertThat(FeedFailure.typeOf(failure)).isEqualTo("IllegalStateException caused by NumberFormatException");
    }
}
