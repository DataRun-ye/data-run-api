package org.nmcpye.datarun.jpa.datasubmission.revision;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.FastCheckService;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A rehearsal of step C's migrations on a dataset shaped like production's, for their cost: run it with
 * {@code -Drevision.rehearsal.rows=67176}. It is skipped otherwise.
 *
 * <p>A fresh database is migrated up to step C, given that many synthetic submissions (form data sized like staging's
 * on 2026-11-24, whose text is 101 characters at the 10th percentile, 648 at the median, 1,874 at the 90th, 15,909 at
 * the 99th, 92,443 at most and 1,212 on average;
 * versions 0 for 99.4%, deleted one in 1,371) and an outbox row for 93% of them, then vacuumed. Each step C changeset
 * is then applied on its own and timed, with the WAL it wrote; then the run-always gap repair with nothing to repair,
 * and after a simulated window of 6.5.5, the release before step C. The numbers go to {@code target/revision-rehearsal.txt}. Synthetic data and a
 * container's default settings: the timings show orders of magnitude, not production's exact numbers.</p>
 */
@EnabledIfSystemProperty(named = "revision.rehearsal.rows", matches = "\\d+")
class SubmissionRevisionRehearsalIT {

    private static final String MASTER = "config/liquibase/master.xml";
    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");
    private static final List<String> REPORT = new ArrayList<>();
    private static final List<String> PLAN = new ArrayList<>();

    private static JdbcTemplate db;

    @BeforeAll
    static void start() {
        PG.start();
        db = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
    }

    @AfterAll
    static void stop() throws IOException {
        PG.stop();
        Files.write(Path.of("target", "revision-rehearsal.txt"), REPORT);
    }

    private static void report(String line) {
        REPORT.add(line);
        System.out.println("[rehearsal] " + line);
    }

    private static Liquibase liquibase(Connection connection) throws Exception {
        ChangeLogHistoryServiceFactory.getInstance().resetAll();
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        return new Liquibase(MASTER, new ClassLoaderResourceAccessor(), database);
    }

    private static String wal() {
        return db.queryForObject("SELECT pg_current_wal_lsn()::text", String.class);
    }

    private static long walSince(String lsn) {
        Long bytes = db.queryForObject("SELECT pg_wal_lsn_diff(pg_current_wal_lsn(), ?::pg_lsn)::bigint", Long.class, lsn);
        return bytes == null ? 0 : bytes;
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.MINUTES)
    void stepCOnAProductionSizedDataset() throws Exception {
        int rows = Integer.getInteger("revision.rehearsal.rows");
        List<String> stepC = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            Liquibase liquibase = liquibase(connection);
            List<ChangeSet> unrun = liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression());
            int first = 0;
            while (!unrun.get(first).getId().startsWith(SubmissionRevisionMigrationIT.STEP_C)) {
                first++;
            }
            for (int i = first; i < unrun.size(); i++) {
                stepC.add(unrun.get(i).getId());
            }
            liquibase.update(first, new Contexts(), new LabelExpression());
        }

        long seeding = System.nanoTime();
        seed(rows);
        db.execute("VACUUM ANALYZE data_submission");
        db.execute("VACUUM ANALYZE outbox");
        report("seeded " + rows + " submissions and " + db.queryForObject("SELECT count(*) FROM outbox", Long.class)
            + " outbox rows in " + seconds(seeding) + " s; data_submission "
            + db.queryForObject("SELECT pg_size_pretty(pg_total_relation_size('data_submission'))", String.class)
            + ", outbox " + db.queryForObject("SELECT pg_size_pretty(pg_total_relation_size('outbox'))", String.class)
            + ", form_data stored " + db.queryForObject(
            "SELECT pg_size_pretty(sum(pg_column_size(form_data))) FROM data_submission", String.class));

        String before = wal();
        long all = System.nanoTime();
        for (String id : stepC) {
            String lsn = wal();
            long start = System.nanoTime();
            try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
                liquibase(connection).update(1, new Contexts(), new LabelExpression());
            }
            report(String.format("%-48s %7.2f s  WAL %6.1f MB", id, seconds(start), walSince(lsn) / 1048576.0));
        }
        report(String.format("step C in all: %.2f s, WAL %.1f MB", seconds(all), walSince(before) / 1048576.0));
        report("journal: " + db.queryForObject("SELECT count(*) FROM journal.submission_revision", Long.class) + " rows, "
            + db.queryForObject("SELECT pg_size_pretty(pg_total_relation_size('journal.submission_revision'))", String.class)
            + " with its key (heap " + db.queryForObject(
            "SELECT pg_size_pretty(pg_relation_size('journal.submission_revision'))", String.class) + ", TOAST "
            + db.queryForObject("SELECT pg_size_pretty(pg_total_relation_size(reltoastrelid)) FROM pg_class "
            + "WHERE oid = 'journal.submission_revision'::regclass", String.class) + ")");
        assertThat(db.queryForObject("SELECT count(*) FROM journal.submission_revision", Long.class)).isEqualTo(rows);

        report("a start right after, no cue (Liquibase run, the gap repair the only changeset): " + startAgain()
            + " s; the gap repair's statement: " + gapRepairPlan(true) + "; if it read every submission: "
            + gapRepairPlan(false));
        db.execute("VACUUM ANALYZE journal.submission_revision");
        report("the same once the journal is vacuumed: " + startAgain() + " s; statement " + gapRepairPlan(true)
            + "; reading every submission " + gapRepairPlan(false));

        // A 6.5.5 window: changed re-sends of about 500 submissions and 500 new ones, without revisions, each with
        // the outbox row 6.5.5 writes (no version): the cue for the next start.
        db.update("""
            UPDATE data_submission SET lock_version = lock_version + 1, last_modified_date = localtimestamp
            WHERE serial_number % 134 = 0""");
        db.update("""
            INSERT INTO data_submission (id, uid, template_uid, template_version_uid, template_version_no, assignment_uid,
                                         activity_uid, team_uid, org_unit_uid, form_data, lock_version, created_by,
                                         created_date, last_modified_by, last_modified_date, deleted)
            SELECT 'W' || lpad(i::text, 25, '0'), 'w' || lpad(i::text, 10, '0'), 'rehForm0001', 'rehVers0001', 1,
                   'rehAsg00001', 'rehActiv001', 'rehTem00001', 'rehOrg00001', '{"main": {}}', 0, 'rehearsal',
                   localtimestamp, 'rehearsal', localtimestamp, false
            FROM generate_series(1, 500) AS i""");
        db.update("""
            INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload, event_type,
                                status, attempt, created_at)
            SELECT id, serial_number, uid, template_version_uid, form_data,
                   CASE WHEN id LIKE 'W%' THEN 'SAVE' ELSE 'UPDATE' END, 'pending', 0, localtimestamp
            FROM data_submission WHERE serial_number % 134 = 0 OR id LIKE 'W%'""");
        long gaps = db.queryForObject("""
            SELECT count(*) FROM data_submission s WHERE NOT EXISTS (
              SELECT 1 FROM journal.submission_revision r WHERE r.submission_id = s.id AND r.version >= s.lock_version)""",
            Long.class);
        String lsn = wal();
        report("the start after a 6.5.5 window with " + gaps + " gaps (its outbox rows are the cue): " + startAgain()
            + " s, WAL " + String.format("%.1f MB", walSince(lsn) / 1048576.0) + "; GAP_REPAIR revisions: "
            + db.queryForObject("SELECT count(*) FROM journal.submission_revision WHERE command = 'GAP_REPAIR'", Long.class));
        assertThat(db.queryForObject("SELECT count(*) FROM journal.submission_revision WHERE command = 'GAP_REPAIR'", Long.class))
            .isEqualTo(gaps);
        report("and the start after that, no cue again: " + startAgain() + " s; statement " + gapRepairPlan(true));
        PLAN.clear();
        gapRepairPlan(true);
        report("its plan: " + String.join(" | ", PLAN));

        // A rollback of step C once the application wrote a revision refuses before it removes anything.
        db.update("""
            INSERT INTO app_user (id, uid, login, password_hash, activated, lang_key, created_by)
            VALUES ('01REHEARSALUSER00000000001', 'rehUser0001', 'rehearsal-user', 'x', true, 'en', 'rehearsal')""");
        db.update("UPDATE data_submission SET lock_version = lock_version + 1 WHERE id = 'R" + "0".repeat(24) + "2'");
        db.update("""
            INSERT INTO journal.submission_revision (submission_id, version, previous_version, kind, command,
                                                     actor_user_id, form_data, metadata)
            SELECT s.id, s.lock_version, s.lock_version - 1, 'UPDATE', 'UPLOAD', '01REHEARSALUSER00000000001',
                   s.form_data, journal.submission_revision_metadata(s)
            FROM data_submission s WHERE s.id = 'R""" + "0".repeat(24) + "2'");
        long revisions = db.queryForObject("SELECT count(*) FROM journal.submission_revision", Long.class);
        int stepCRows = db.queryForObject(
            "SELECT count(*) FROM databasechangelog WHERE id LIKE '" + SubmissionRevisionMigrationIT.STEP_C + "%'",
            Integer.class);
        long rollingBack = System.nanoTime();
        String refusal = "accepted";
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            liquibase(connection).rollback(stepCRows, (String) null);
        } catch (Exception e) {
            refusal = e.toString().contains("never roll it back") ? "refused" : "failed otherwise: " + e;
        }
        long triggers = db.queryForObject("""
            SELECT count(*) FROM pg_trigger WHERE tgrelid = 'journal.submission_revision'::regclass
            AND NOT tgisinternal""", Long.class);
        long outboxKey = db.queryForObject(
            "SELECT count(*) FROM pg_constraint WHERE conname = 'fk_outbox_submission_revision'", Long.class);
        report(String.format("a rollback of step C with an application revision present: %s in %.2f s; afterwards %d "
                + "triggers, %d outbox key, revisions %s", refusal, seconds(rollingBack), triggers, outboxKey,
            db.queryForObject("SELECT count(*) FROM journal.submission_revision", Long.class) == revisions
                ? "unchanged" : "CHANGED"));
        assertThat(refusal).isEqualTo("refused");
        assertThat(triggers).isEqualTo(3);
        assertThat(outboxKey).isEqualTo(1);
        startAgain();

        PG.copyFileToContainer(org.testcontainers.utility.MountableFile.forHostPath(
            Path.of("scripts", "integrity", "sql", "11-revisions.sql")), "/tmp/11-revisions.sql");
        PG.copyFileToContainer(org.testcontainers.images.builder.Transferable.of("""
            BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
            SET LOCAL work_mem = '8MB';
            SET LOCAL max_parallel_workers_per_gather = 0;
            SET LOCAL ROLE pg_read_all_data;
            \\timing on
            \\i /tmp/11-revisions.sql
            ROLLBACK;
            """), "/tmp/check-11.sql");
        var check = PG.execInContainer("psql", "-X", "-q", "-A", "-t", "-v", "ON_ERROR_STOP=1",
            "-U", PG.getUsername(), "-d", PG.getDatabaseName(), "-f", "/tmp/check-11.sql");
        assertThat(check.getExitCode()).as(check.getStderr()).isZero();
        report("integrity check 11 on this dataset: " + check.getStdout().lines()
            .filter(line -> line.startsWith("Time: ") || line.matches("11[a-h]\\|.*"))
            .map(line -> line.replaceAll("\\|[^|]*$", "")).toList());
    }

    /** One Liquibase run of the whole changelog, timed: only the run-always gap repair has anything to do. */
    private static String startAgain() throws Exception {
        long start = System.nanoTime();
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())) {
            liquibase(connection).update(new Contexts(), new LabelExpression());
        }
        return String.format("%.2f", seconds(start));
    }

    /**
     * EXPLAIN ANALYZE of the gap repair's read, as changeset c08 has it: with its cue, as at a start, or without it,
     * as if it read every submission. Its execution time, and whether the scan of data_submission ran.
     */
    private static String gapRepairPlan(boolean withCue) {
        String cue = withCue ? """
            (coalesce((SELECT max(o.outbox_id) FROM outbox o WHERE o.submission_version IS NULL), 0)
                 > coalesce((SELECT w.outbox_id FROM journal.outbox_watermark w), 0)
             OR coalesce((SELECT c.id FROM databasechangelog c ORDER BY c.orderexecuted DESC LIMIT 1), '')
                <> '20261124-c08-submission-revision-gap-repair') AND """ : "";
        String explain = """
            EXPLAIN (ANALYZE, BUFFERS, SUMMARY ON)
            SELECT s.id, s.lock_version,
                   (SELECT max(h.version) FROM journal.submission_revision h WHERE h.submission_id = s.id),
                   s.form_data, journal.submission_revision_metadata(s)
            FROM data_submission s
            WHERE %s NOT EXISTS (SELECT 1 FROM journal.submission_revision r
                                 WHERE r.submission_id = s.id AND r.version >= s.lock_version)""".formatted(cue);
        List<String> plan = new ArrayList<>();
        // As the changeset runs it: without JIT.
        try (Connection connection = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("SET jit = off");
            try (var rows = statement.executeQuery(explain)) {
                while (rows.next()) {
                    plan.add(rows.getString(1));
                }
            }
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
        PLAN.addAll(plan);
        boolean skipped = plan.stream().anyMatch(line -> line.contains("Seq Scan on data_submission")
            && line.contains("never executed"));
        return plan.stream().filter(line -> line.startsWith("Execution Time"))
            .map(line -> line.replaceAll("[^0-9.]", "")).findFirst().orElse("?") + " ms"
            + (skipped ? " (data_submission never read)" : "");
    }

    private static double seconds(long startNanos) {
        return (System.nanoTime() - startNanos) / 1e9;
    }

    /**
     * Submissions shaped like staging's: the share of documents per size comes from a fixed hash of the row number,
     * and a document's size from its number of repeat rows (about 190 characters each, with the keys phones send).
     */
    private static void seed(int rows) {
        db.update("""
            INSERT INTO data_submission (id, uid, template_uid, template_version_uid, template_version_no,
                                         assignment_uid, activity_uid, team_uid, team_code, org_unit_uid,
                                         org_unit_code, org_unit_name, status, start_entry_time, finished_entry_time,
                                         form_data, lock_version, created_by, created_date, last_modified_by,
                                         last_modified_date, deleted, deleted_at)
            SELECT 'R' || lpad(i::text, 25, '0'), 'r' || lpad(i::text, 10, '0'), 'rehForm0001', 'rehVers0001', 1,
                   'rehAsg' || lpad((i % 600)::text, 5, '0'), 'rehActiv001', 'rehTem' || lpad((i % 600)::text, 5, '0'),
                   'T' || (i % 600), 'rehOrg' || lpad((i % 3000)::text, 5, '0'), 'OU' || (i % 3000),
                   'village number ' || (i % 3000), 'COMPLETED', t.at, t.at + interval '10 minutes', f.data,
                   CASE WHEN i % 1000 < 994 THEN 0 WHEN i % 1000 < 998 THEN 1 ELSE 2 + i % 17 END,
                   'rehearsal', t.at, 'rehearsal', t.at + interval '1 hour', i % 1371 = 0,
                   CASE WHEN i % 1371 = 0 THEN t.at + interval '2 hours' END
            FROM generate_series(1, ?) AS i
            CROSS JOIN LATERAL (SELECT timestamp '2025-04-22 00:00' + i * interval '7 minutes' AS at) t
            CROSS JOIN LATERAL (SELECT CASE WHEN i = 1 THEN 640
                                            WHEN (i::bigint * 2654435761) % 1000 < 100 THEN 0
                                            WHEN (i::bigint * 2654435761) % 1000 < 500 THEN 3
                                            WHEN (i::bigint * 2654435761) % 1000 < 700 THEN 6
                                            WHEN (i::bigint * 2654435761) % 1000 < 900 THEN 11
                                            WHEN (i::bigint * 2654435761) % 1000 < 950 THEN 20
                                            WHEN (i::bigint * 2654435761) % 1000 < 990 THEN 50
                                            ELSE 140 END AS repeats) n
            CROSS JOIN LATERAL (
                SELECT jsonb_build_object(
                           'main', jsonb_build_object('name', 'household ' || i, 'size', i % 9),
                           'rows', coalesce(jsonb_agg(jsonb_build_object(
                               '_id', md5(i || '-' || r), '_index', r, '_parentId', 'r' || i,
                               '_submissionUid', 'r' || lpad(i::text, 10, '0'), 'item', 'net ' || (r % 5),
                               'note', substr(md5(r || '-' || i), 1, 12))), '[]'::jsonb)) AS data
                FROM generate_series(1, n.repeats) AS r) f""", rows);
        db.update("""
            INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload, event_type,
                                status, attempt, created_at)
            SELECT id, serial_number, uid, template_version_uid, form_data, 'SAVE', 'success', 0, created_date
            FROM data_submission WHERE serial_number % 14 <> 0""");
        Map<String, Object> shape = db.queryForMap("""
            SELECT percentile_disc(ARRAY[0.1, 0.5, 0.9, 0.99, 1.0]) WITHIN GROUP (ORDER BY length(form_data::text))::text
                       AS text_length_percentiles,
                   round(avg(length(form_data::text))) AS text_length_avg,
                   count(*) FILTER (WHERE deleted) AS deleted,
                   count(*) FILTER (WHERE lock_version > 0) AS changed
            FROM data_submission""");
        report("synthetic shape: " + shape);
    }
}
