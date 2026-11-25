package org.nmcpye.datarun.submission.feed.publisher;

import org.nmcpye.datarun.config.SqlTestContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs integrity check 12 ({@code scripts/integrity/sql/12-feed.sql}) with psql in the test database's container, the
 * way {@code scripts/integrity/check.sh} runs it: in a read-only, repeatable-read transaction, as
 * {@code pg_read_all_data}, with the check's statement settings.
 *
 * @param totals each line's count, summed over its scopes
 * @param scoped each scoped line's count, by {@code key|scope}
 */
record IntegrityCheck12(Map<String, Long> totals, Map<String, Long> scoped) {

    static IntegrityCheck12 run(SqlTestContainer database) throws Exception {
        JdbcDatabaseContainer<?> container = database.getTestContainer();
        container.copyFileToContainer(MountableFile.forHostPath(Path.of("scripts", "integrity", "sql", "12-feed.sql")),
            "/tmp/12-feed.sql");
        container.copyFileToContainer(Transferable.of("""
            \\set ON_ERROR_STOP 1
            BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
            SET LOCAL work_mem = '8MB';
            SET LOCAL max_parallel_workers_per_gather = 0;
            SET LOCAL statement_timeout = '5min';
            SET LOCAL ROLE pg_read_all_data;
            \\i /tmp/12-feed.sql
            ROLLBACK;
            """), "/tmp/check-12.sql");
        var result = container.execInContainer("psql", "-X", "-q", "-A", "-t", "-v", "ON_ERROR_STOP=1",
            "-U", container.getUsername(), "-d", container.getDatabaseName(), "-f", "/tmp/check-12.sql");
        assertThat(result.getExitCode()).as(result.getStderr()).isZero();
        Map<String, Long> totals = new TreeMap<>();
        Map<String, Long> scoped = new TreeMap<>();
        for (String line : result.getStdout().split("\n")) {
            String[] cells = line.split("\\|", -1);
            if (cells.length >= 3 && cells[0].matches("12[a-z]")) {
                long n = Long.parseLong(cells[1]);
                totals.merge(cells[0], n, Long::sum);
                if (cells.length >= 4 && !cells[3].isEmpty()) {
                    scoped.merge(cells[0] + "|" + cells[3], n, Long::sum);
                }
            }
        }
        return new IntegrityCheck12(totals, scoped);
    }

    long line(String key) {
        Long n = totals.get(key);
        assertThat(n).as("line %s", key).isNotNull();
        return n;
    }
}
