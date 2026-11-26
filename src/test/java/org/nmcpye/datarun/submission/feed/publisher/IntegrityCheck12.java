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
 * Runs integrity check 12 with psql in the test database's container, the way {@code scripts/integrity/check.sh} runs
 * it: in the transaction {@code begin-read-only.sql} opens, under the same limits ({@code work_mem},
 * {@code temp_file_limit}, {@code statement_timeout} and the others) and as {@code pg_read_all_data};
 * {@code 12-feed.sql} once, then {@code feed-rows-by-form.sql} once per form the stored submissions name.
 *
 * @param totals each line's count, summed over its scopes
 * @param scoped each scoped line's count other than 0, by {@code key|scope}: the scope a line names, else the form
 *               its run was for
 */
record IntegrityCheck12(Map<String, Long> totals, Map<String, Long> scoped) {

    private static final Path SQL = Path.of("scripts", "integrity", "sql");

    static IntegrityCheck12 run(SqlTestContainer database) throws Exception {
        JdbcDatabaseContainer<?> container = database.getTestContainer();
        for (String file : new String[]{"begin-read-only.sql", "12-feed.sql", "feed-rows-by-form.sql"}) {
            container.copyFileToContainer(MountableFile.forHostPath(SQL.resolve(file)), "/tmp/" + file);
        }
        StringBuilder script = new StringBuilder("""
            \\set ON_ERROR_STOP 1
            \\i /tmp/begin-read-only.sql
            \\echo @check 12-feed
            \\i /tmp/12-feed.sql
            \\echo @check feed-rows-by-form
            """);
        for (String form : psql(container, "-c",
            "SELECT DISTINCT template_uid FROM data_submission WHERE template_uid IS NOT NULL ORDER BY 1").split("\n")) {
            if (!form.isEmpty()) {
                script.append("\\set tmpl '").append(form.replace("'", "''")).append("'\n")
                    .append("\\echo @template ").append(form).append('\n')
                    .append("\\i /tmp/feed-rows-by-form.sql\n");
            }
        }
        script.append("ROLLBACK;\n");
        container.copyFileToContainer(Transferable.of(script.toString()), "/tmp/check-12.sql");

        Map<String, Long> totals = new TreeMap<>();
        Map<String, Long> scoped = new TreeMap<>();
        String form = "";
        for (String line : psql(container, "-f", "/tmp/check-12.sql").split("\n")) {
            if (line.startsWith("@check ")) {
                form = "";
                continue;
            }
            if (line.startsWith("@template ")) {
                form = line.substring("@template ".length());
                continue;
            }
            String[] cells = line.split("\\|", -1);
            if (cells.length >= 3 && cells[0].matches("12[a-z]")) {
                long n = Long.parseLong(cells[1]);
                totals.merge(cells[0], n, Long::sum);
                String scope = cells.length >= 4 && !cells[3].isEmpty() ? cells[3] : form;
                if (!scope.isEmpty() && n != 0) {
                    scoped.merge(cells[0] + "|" + scope, n, Long::sum);
                }
            }
        }
        return new IntegrityCheck12(totals, scoped);
    }

    private static String psql(JdbcDatabaseContainer<?> container, String option, String argument) throws Exception {
        var result = container.execInContainer("psql", "-X", "-q", "-A", "-t", "-v", "ON_ERROR_STOP=1",
            "-U", container.getUsername(), "-d", container.getDatabaseName(), option, argument);
        assertThat(result.getExitCode()).as(result.getStderr()).isZero();
        return result.getStdout();
    }

    long line(String key) {
        Long n = totals.get(key);
        assertThat(n).as("line %s", key).isNotNull();
        return n;
    }
}
