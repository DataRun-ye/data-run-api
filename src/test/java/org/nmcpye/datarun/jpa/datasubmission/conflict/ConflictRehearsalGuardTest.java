package org.nmcpye.datarun.jpa.datasubmission.conflict;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datasubmission.conflict.ConflictRehearsal.FailureStreak;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.nmcpye.datarun.jpa.datasubmission.conflict.ConflictRehearsal.requireAClone;

/**
 * The rehearsal writes and rolls back, so it runs on a clone only: what it refuses before any connection. It also
 * runs every stored submission through production code in one process, so its own classes refer to no Mockito type.
 */
class ConflictRehearsalGuardTest {

    /**
     * A mock remembers every call it answers. The repeat-id walk asks a template context for its paths once for every
     * key of every document, and a cached mock context ran the rehearsal out of memory at about 30,000 submissions. The
     * rule covers the rehearsal's own classes, nested and anonymous ones included, not code they call.
     */
    @Test
    void theRehearsalUsesNoMock() throws Exception {
        // Only the rehearsal's own class files, nested and anonymous ones included, and what they name is not looked up
        // on the classpath: a dependency's package is known from its name, and the lookup costs seconds.
        Pattern rehearsal = Pattern.compile(".*/ConflictRehearsal(\\$[^/]*)?\\.class$");
        Path directory = Path.of(ConflictRehearsal.class.getResource("").toURI());
        JavaClasses classes = ArchConfiguration.withThreadLocalScope(configuration -> {
            configuration.setResolveMissingDependenciesFromClassPath(false);
            return new ClassFileImporter().withImportOption(location -> location.matches(rehearsal))
                .importPath(directory);
        });
        assertThat(classes.contain(ConflictRehearsal.class)).isTrue();
        noClasses().should().dependOnClassesThat().resideInAPackage("org.mockito..").check(classes);
    }

    /**
     * A pass counts each failed submission and goes on. A success resets the count until twenty fail in a row; from
     * then on the pass is stopped for good, even if the database comes back.
     */
    @Test
    void aSuccessResetsTheCountUntilTwentyFailuresInARowStopThePassForGood() {
        FailureStreak streak = new FailureStreak();
        RuntimeException failure = new IllegalArgumentException("unreachable");
        for (int i = 1; i < FailureStreak.LIMIT; i++) {
            streak.after(failure);
        }
        streak.after(null);
        assertThat(streak.stopped()).isFalse();
        for (int i = 1; i < FailureStreak.LIMIT; i++) {
            streak.after(failure);
        }
        assertThatThrownBy(() -> streak.after(failure)).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("20 submissions failed in a row").hasCause(failure);
        streak.after(null);
        assertThat(streak.stopped()).isTrue();
        assertThat(streak.failure()).get().extracting(Throwable::getCause).isSameAs(failure);
    }

    /**
     * A worker that fails outside a submission's own count stops the others before their next submission, and its own
     * failure is the one reported. It is worker 1's, not worker 0's, whose result is collected first; each other
     * worker's first submission waits until it sees the stop, so the bound is exact.
     */
    @Test
    void aWorkerFailingOutrightStopsTheOthersAndItsFailureIsTheOneReported() {
        FailureStreak streak = new FailureStreak();
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger sawTheStop = new AtomicInteger();
        int threads = 4;
        assertThatThrownBy(() -> ConflictRehearsal.inParallel(ids(2_000), threads, streak, id -> {
            attempts.incrementAndGet();
            if (id.equals("1")) {
                throw new IllegalArgumentException("a failure outside the submission's own count");
            }
            if (stopsWithin(streak, 5)) {
                sawTheStop.incrementAndGet();
            }
        })).isInstanceOf(IllegalStateException.class).hasMessageContaining("A rehearsal worker failed")
            .cause().isInstanceOf(IllegalArgumentException.class)
            .hasMessage("a failure outside the submission's own count");
        assertThat(attempts.get()).isBetween(1, threads);
        assertThat(sawTheStop.get()).isEqualTo(attempts.get() - 1);
    }

    /** Whether the pass stops within the given seconds. */
    private static boolean stopsWithin(FailureStreak streak, int seconds) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < deadline) {
            if (streak.stopped()) {
                return true;
            }
            Thread.onSpinWait();
        }
        return false;
    }

    /**
     * With the database down for good, every worker stops soon after the twentieth failure, none going on alone, and
     * the stop's own failure is the one reported.
     */
    @Test
    void whenEverySubmissionFailsAllWorkersStopWithinOneSubmissionEachOfTheLimit() {
        FailureStreak streak = new FailureStreak();
        AtomicInteger attempts = new AtomicInteger();
        int threads = 4;
        assertThatThrownBy(() -> ConflictRehearsal.inParallel(ids(2_000), threads, streak, id -> {
            attempts.incrementAndGet();
            streak.after(new IllegalStateException("database unreachable"));
        })).isInstanceOf(IllegalStateException.class).hasMessageContaining("A rehearsal worker failed")
            .cause().hasMessageContaining("20 submissions failed in a row");
        assertThat(attempts.get()).isBetween(FailureStreak.LIMIT, FailureStreak.LIMIT + threads - 1);
    }

    /**
     * When several workers fail, the first failure is the one reported: worker 1 fails first, then a submission still
     * running on worker 0 fails after it has seen the stop.
     */
    @Test
    void theFirstFailureIsTheOneReportedNotALaterOne() {
        FailureStreak streak = new FailureStreak();
        IllegalArgumentException first = new IllegalArgumentException("first");
        IllegalArgumentException later = new IllegalArgumentException("later");
        assertThatThrownBy(() -> ConflictRehearsal.inParallel(ids(4), 2, streak, id -> {
            if (id.equals("1")) {
                throw first;
            }
            if (id.equals("0") && stopsWithin(streak, 5)) {
                throw later;
            }
        })).isInstanceOf(IllegalStateException.class).cause().isSameAs(first);
    }

    /** Once a pass has stopped, no worker takes another submission, and the pass still reports why it stopped. */
    @Test
    void aStoppedPassTakesNoMoreSubmissions() {
        FailureStreak streak = new FailureStreak();
        IllegalStateException earlier = new IllegalStateException("database unreachable");
        streak.stop(earlier);
        AtomicInteger attempts = new AtomicInteger();
        assertThatThrownBy(() -> ConflictRehearsal.inParallel(ids(100), 4, streak, id -> attempts.incrementAndGet()))
            .isInstanceOf(IllegalStateException.class).hasCause(earlier);
        assertThat(attempts.get()).isZero();
    }

    private static List<String> ids(int count) {
        return IntStream.range(0, count).mapToObj(String::valueOf).toList();
    }

    @Test
    void productionsHostAndDatabaseAreRefusedWithNoOverride() {
        for (String host : List.of("datarun-prod-db.lab", "DATARUN-PROD-DB.LAB", "datarun-prod-db", "192.168.1.111")) {
            assertThatThrownBy(() -> requireAClone(settings("jdbc:postgresql://" + host + ":5432/clone", "clone")))
                .as(host).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("production");
        }
        assertThatThrownBy(() -> requireAClone(settings("jdbc:postgresql://localhost:5432/nmcpdb", "nmcpdb")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("production");
    }

    @Test
    void aDatabaseMustBeNamedAndTheHostReadable() {
        assertThatThrownBy(() -> requireAClone(settings("jdbc:postgresql://localhost:5432/clone", " ")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Name the database");
        assertThatThrownBy(() -> requireAClone(settings("jdbc:mysql://localhost:3306/clone", "clone")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PostgreSQL");
        assertThatThrownBy(() -> requireAClone(settings("jdbc:postgresql:clone", "clone")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("host");
        assertThatCode(() -> requireAClone(settings("jdbc:postgresql://localhost:5432/clone", "clone")))
            .doesNotThrowAnyException();
    }

    private static ConflictRehearsal.Settings settings(String url, String database) {
        return new ConflictRehearsal.Settings(url, "user", "password", database, null, 200, 4, "s2", null);
    }
}
