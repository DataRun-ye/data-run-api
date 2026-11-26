package org.nmcpye.datarun.submission.feed.agreement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Map;

/**
 * The agreement harness as one command, for a lab host: {@code scripts/feed/agreement-bundle.sh} builds a bundle (the
 * application's classes, this harness, its runtime libraries and the oracle's SQL), and {@code agreement.sh} in it runs
 * this class there, detached, with the settings of a file the operator names. Nothing here needs Maven or a test
 * runner, and nothing reaches the database from another machine.
 *
 * <p>The counts go to the file {@code FEED_AGREEMENT_REPORT} names ({@code feed-agreement.txt} when unset). Exit 0 when
 * every agreement line reads 0, 1 when one does not, 2 when the settings refuse the run (a setting missing or
 * malformed, no database named, another database than the one named, production without leave:
 * {@link FeedAgreement.Refused}), 3 when the database or the run failed, whatever the exception or error.</p>
 */
public final class FeedAgreementMain {

    private FeedAgreementMain() {
    }

    public static void main(String[] args) {
        System.exit(run(System.getenv()));
    }

    /** The run, and its exit status. */
    static int run(Map<String, String> environment) {
        return run(environment, FeedAgreementMain::measure);
    }

    /** What measures: {@link #measure}, or another to prove the exit statuses. */
    interface Measure {
        FeedAgreement.Report measure(Map<String, String> environment, Path out) throws Exception;
    }

    /** The run with the given measure, and its exit status: 2 for a refusal only, 3 for any failure. */
    static int run(Map<String, String> environment, Measure measure) {
        Path out = Path.of(environment.getOrDefault("FEED_AGREEMENT_REPORT", "feed-agreement.txt"));
        try {
            FeedAgreement.Report report = measure.measure(environment, out);
            System.out.println("Counts written to " + out.toAbsolutePath());
            return report.agrees() ? 0 : 1;
        } catch (FeedAgreement.Refused refused) {
            System.err.println("The run refused: " + refused.getMessage());
            return 2;
        } catch (java.sql.SQLException failed) {
            // the database's own message may quote a stored value, so only its type and state
            System.err.println("The run failed in the database: " + failed.getClass().getSimpleName() + " [SQLState "
                + failed.getSQLState() + "]");
            return 3;
        } catch (Exception | Error failed) {
            // an Error too, such as a class missing from the bundle: never exit 1, which reads as a disagreement
            System.err.println("The run failed: " + failed.getClass().getSimpleName());
            return 3;
        }
    }

    /** Measures the database the environment names and writes the counts to {@code out}. */
    static FeedAgreement.Report measure(Map<String, String> environment, Path out) throws Exception {
        FeedAgreement.Settings settings = FeedAgreement.Settings.fromEnvironment(environment);
        FeedAgreement.Report report;
        try (Connection connection = FeedAgreement.connect(settings)) {
            report = FeedAgreement.run(connection, settings);
        }
        Path parent = out.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(out, report.text());
        return report;
    }
}
