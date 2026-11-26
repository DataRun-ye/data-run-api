package org.nmcpye.datarun.submission.feed.agreement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The command's exit statuses: 2 when its settings refuse the run, and only then; 3 for any failure during the run,
 * whatever the exception's type, an Error included. The refusals here need no database: each comes before any
 * connection.
 */
class FeedAgreementMainTest {

    @TempDir
    Path directory;

    private Map<String, String> reportOnly() {
        return Map.of("FEED_AGREEMENT_REPORT", directory.resolve("counts.txt").toString());
    }

    private Map<String, String> settings(String jdbcUrl, String batch) {
        return Map.of(
            "FEED_AGREEMENT_JDBC_URL", jdbcUrl,
            "FEED_AGREEMENT_DATABASE", "clone",
            "FEED_AGREEMENT_BATCH", batch,
            "FEED_AGREEMENT_REPORT", directory.resolve("counts.txt").toString());
    }

    @Test
    void aFailureDuringTheRunExitsThreeWhateverItsType() {
        assertThat(FeedAgreementMain.run(reportOnly(), (environment, out) -> {
            throw new IllegalStateException("The oracle returned a value that is not JSON");
        })).as("an illegal state met while measuring").isEqualTo(3);
        assertThat(FeedAgreementMain.run(reportOnly(), (environment, out) -> {
            throw new IllegalArgumentException("a value the run could not read");
        })).as("an illegal argument met while measuring").isEqualTo(3);
        assertThat(FeedAgreementMain.run(reportOnly(), (environment, out) -> {
            throw new SQLException("canceled", "57014");
        })).as("the database").isEqualTo(3);
        assertThat(FeedAgreementMain.run(reportOnly(), (environment, out) -> {
            throw new NoClassDefFoundError("org/nmcpye/datarun/submission/normalized/SubmissionNormalizer");
        })).as("an Error, such as a class missing from the bundle").isEqualTo(3);
        assertThat(FeedAgreementMain.run(reportOnly(), (environment, out) -> {
            throw new FeedAgreement.Refused("Name the database to measure in FEED_AGREEMENT_DATABASE");
        })).as("a refusal").isEqualTo(2);
    }

    @Test
    void missingOrMalformedSettingsAreRefusedBeforeAnyConnection() {
        assertThat(FeedAgreementMain.run(reportOnly())).as("no JDBC URL").isEqualTo(2);
        assertThat(FeedAgreementMain.run(settings("jdbc:postgresql://clone.invalid:5432/clone", "many")))
            .as("a batch that is not a number").isEqualTo(2);
        assertThat(FeedAgreementMain.run(settings("jdbc:postgresql://clone.invalid:5432/clone", "0")))
            .as("an empty batch").isEqualTo(2);
        assertThat(FeedAgreementMain.run(settings("jdbc:mysql://clone.invalid:3306/clone", "500")))
            .as("not PostgreSQL").isEqualTo(2);
        assertThat(FeedAgreementMain.run(settings("jdbc:postgresql://datarun-prod-db.lab:5432/clone", "500")))
            .as("production's host without leave").isEqualTo(2);
        assertThat(directory.resolve("counts.txt")).as("no counts written").doesNotExist();
    }
}
