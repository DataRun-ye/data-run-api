package org.nmcpye.datarun.jpa.datasubmissionbatching.mongomigrationrunner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;

import java.time.Instant;

/**
 * MigrationJobLauncher - gated one-shot runner for migration.
 * <p>
 * Usage:
 * - By default it does nothing.
 * - To run: pass JVM arg or env property `migration.run=true` (or set migration.auto-start=true).
 * - Optionally tune chunk/page sizes via properties already in your config.
 * <p>
 * This runner will launch the configured `mongoToPostgresJob` and wait for completion,
 * logging summary info. It is safe to keep in the codebase because it is disabled unless
 * `migration.run=true`.
 *
 * @author Hamza Assada 16/08/2025 (7amza.it@gmail.com)
 */
//@Component
//@Order(1500)
@Slf4j
@RequiredArgsConstructor
public class MigrationMongoSubmissionJobLauncher implements CommandLineRunner {
    private final JobLauncher jobLauncher;
    private final Job mongoToPostgresJob;
    private final Environment environment;

    /**
     * Property to gate the runner. You can pass --migration.run=true or set env var.
     * Also accepts migration.auto-start (legacy alias).
     */
    @Value("${migration.run:false}")
    private boolean migrationRun;

    @Value("${migration.auto-start:false}")
    private boolean migrationAutoStart;

    @Override
    public void run(String... args) throws Exception {
        // gate: only run when explicitly asked
        boolean shouldRun = migrationRun || migrationAutoStart
            || Boolean.parseBoolean(environment.getProperty("MIGRATION_RUN", "false"))
            || Boolean.parseBoolean(environment.getProperty("MIGRATION_AUTO_START", "false"));

        if (!shouldRun) {
            log.info("Migration runner disabled. To start migration set `migration.run=true` (or migration.auto-start=true).");
            return;
        }

        log.info("Migration runner enabled — starting job mongoToPostgresJob.");

        try {
            JobParameters params = new JobParametersBuilder()
                .addLong("ts", Instant.now().toEpochMilli())
                .toJobParameters();

            JobExecution execution = jobLauncher.run(mongoToPostgresJob, params);

            // Wait for the job to finish if it's running async in your environment
            // (JobLauncher typically launches synchronously)
            BatchStatus status = execution.getStatus();
            log.info("Migration job started: id={}, status={}", execution.getId(), status);

            // Poll the job execution status until it's finished (optional but helpful for CLI runs)
            while (execution.isRunning() || execution.getStatus().isRunning()) {
                log.info("Migration job is running... stepStatuses={}", execution.getStepExecutions());
                Thread.sleep(1000L); // small sleep; tune if you want longer intervals
            }

            log.info("Migration job finished with status: {}, exitCode: {}, exitMessage: {}",
                execution.getStatus(), execution.getExitStatus().getExitCode(),
                trim(execution.getExitStatus().getExitDescription(), 1000));

        } catch (JobExecutionException jee) {
            log.error("Migration job failed to start or failed during execution.", jee);
            throw jee;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Migration job launcher interrupted while waiting for job to finish.", ie);
        }
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}
