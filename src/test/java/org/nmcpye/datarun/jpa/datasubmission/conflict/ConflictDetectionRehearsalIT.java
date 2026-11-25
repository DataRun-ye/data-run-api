package org.nmcpye.datarun.jpa.datasubmission.conflict;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conflict detection's rehearsal ({@link ConflictRehearsal}) against a database the operator names, for step S2's
 * staging rehearsal. It is skipped unless {@code conflict.rehearsal.url} is set:
 * <pre>
 * CONFLICT_REHEARSAL_PASSWORD=... ./mvnw -Pprod verify -Dtest=NoSuchUnitTest -Dsurefire.failIfNoSpecifiedTests=false \
 *   -Dit.test=ConflictDetectionRehearsalIT -Dfailsafe.failIfNoSpecifiedTests=false \
 *   -Dconflict.rehearsal.url=jdbc:postgresql://HOST:PORT/DATABASE -Dconflict.rehearsal.user=USER
 * </pre>
 * The database must hold step S2's schema (its migrations run). Optional settings: {@code conflict.rehearsal.limit}
 * (submissions in the re-send pass; all by default), {@code conflict.rehearsal.sample} (changed copies; 200),
 * {@code conflict.rehearsal.threads} (connections at work; 4) and {@code conflict.rehearsal.seed} (the sample's seed;
 * {@code s2}). The password is read from the environment only, so it never shows in a process list.
 *
 * <p>The report, counts only, is printed and written to {@code target/conflict-rehearsal.txt}. The test fails when an
 * expectation is not met: nothing kept aside and nothing failed in the re-send pass, and every changed copy kept
 * aside.</p>
 */
@EnabledIfSystemProperty(named = "conflict.rehearsal.url", matches = "jdbc:postgresql:.+")
class ConflictDetectionRehearsalIT {

    @Test
    @Timeout(value = 12, unit = TimeUnit.HOURS)
    void conflictDetectionOnTheNamedDatabase() throws IOException {
        ConflictRehearsal.Settings settings = new ConflictRehearsal.Settings(
            System.getProperty("conflict.rehearsal.url"),
            required(System.getProperty("conflict.rehearsal.user"), "conflict.rehearsal.user"),
            required(System.getenv("CONFLICT_REHEARSAL_PASSWORD"),
                "the environment variable CONFLICT_REHEARSAL_PASSWORD"),
            Integer.getInteger("conflict.rehearsal.limit"),
            Integer.getInteger("conflict.rehearsal.sample", 200),
            Integer.getInteger("conflict.rehearsal.threads", 4),
            System.getProperty("conflict.rehearsal.seed", "s2"),
            null);
        try (ConflictRehearsal rehearsal = new ConflictRehearsal(settings)) {
            try {
                rehearsal.run();
            } finally {
                Files.write(Path.of("target", "conflict-rehearsal.txt"), rehearsal.report());
            }
            assertThat(rehearsal.unchangedConflicts()).as("re-sent unchanged, kept aside").isZero();
            assertThat(rehearsal.unchangedFailures()).as("re-sent unchanged, failed").isZero();
            assertThat(rehearsal.changedNotKeptAside()).as("changed copies not kept aside").isZero();
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Set " + name);
        }
        return value;
    }
}
