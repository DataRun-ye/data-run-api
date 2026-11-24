package org.nmcpye.datarun.etl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.nmcpye.datarun.etl.admin.ScheduledOrchestratorRunner;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.etl.EtlFixture.TEMPLATE;

/**
 * Measures how fast the ETL drains a burst of outbox rows, on synthetic submissions. Off by default; run with
 * {@code -Detl.throughput=true}, and optionally {@code -Detl.throughput.rows}, {@code .members} (repeat rows
 * per submission), {@code .extraFields} (field definitions absent from the payload, as optional fields are),
 * {@code .batch} and {@code .history} (already processed outbox rows, which the claim reads past).
 * {@link #drainABurst} prints one {@code ETL_THROUGHPUT} line, {@link #simulatedPeak} one {@code ETL_PEAK} line,
 * with counts and timings only.
 */
@EtlIntegrationTest
@EnabledIfSystemProperty(named = "etl.throughput", matches = "true")
class EtlThroughputIT {

    private static final int WARM_UP_ROWS = 100;

    @Autowired
    private EtlOrchestrator orchestrator;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ScheduledOrchestratorRunner runner;

    @Autowired
    private EtlProperties properties;

    private EtlFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new EtlFixture(jdbc, transactionManager);
        fixture.clean();
    }

    @AfterEach
    void tearDown() {
        fixture.clean();
    }

    private static int setting(String name, int fallback) {
        return Integer.getInteger("etl.throughput." + name, fallback);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    void drainABurst() {
        int rows = setting("rows", 2000);
        int members = setting("members", 6);
        int extraFields = setting("extraFields", 100);
        int batch = setting("batch", 100);
        int history = setting("history", 0);

        fixture.defineTemplate(TEMPLATE);
        defineExtraFields(extraFields);
        insertHistory(history);
        insertBurst(WARM_UP_ROWS + rows, members);

        drain(batch, WARM_UP_ROWS);
        long start = System.nanoTime();
        drain(batch, Integer.MAX_VALUE);
        double seconds = (System.nanoTime() - start) / 1e9;

        Map<String, Object> outcome = jdbc.queryForMap("""
            SELECT count(*) FILTER (WHERE status = 'success') AS succeeded,
                   count(*) FILTER (WHERE status <> 'success') AS other
            FROM outbox WHERE submission_id NOT LIKE 'history%'""", Map.of());
        long tallRows = jdbc.queryForObject("SELECT count(*) FROM analytics.tall_canonical", Map.of(), Long.class);
        System.out.printf("ETL_THROUGHPUT rows=%d members=%d tallRowsPerSubmission=%d extraFields=%d batch=%d "
                + "history=%d seconds=%.2f msPerRow=%.2f rowsPerHour=%.0f%n",
            rows, members, tallRows / (WARM_UP_ROWS + rows), extraFields, batch, history, seconds,
            seconds * 1000 / rows, rows / seconds * 3600);
        assertThat(outcome).containsEntry("succeeded", (long) (WARM_UP_ROWS + rows)).containsEntry("other", 0L);
    }

    /**
     * A burst written at once and cleared by the scheduled runner as production runs it: polls with the
     * configured delay between them, each draining batches for up to the poll duration. {@code .pollDelay} and
     * {@code .maxPoll} (seconds) and {@code .batch} override the configuration. {@code .slowerMsPerRow} makes
     * each row take that much longer inside the database, standing in for a slower database than the local one.
     * The first poll starts at once; in production a burst may wait up to one poll delay more.
     */
    @Test
    @Timeout(value = 60, unit = TimeUnit.MINUTES)
    void simulatedPeak() throws InterruptedException {
        int rows = setting("rows", 2000);
        int members = setting("members", 6);
        int history = setting("history", 0);
        Duration pollDelay = Duration.ofSeconds(setting("pollDelay", (int) properties.getPollDelay().toSeconds()));
        int configuredBatch = properties.getBatchSize();
        Duration configuredMaxPoll = properties.getMaxPollDuration();
        properties.setBatchSize(setting("batch", configuredBatch));
        properties.setMaxPollDuration(Duration.ofSeconds(setting("maxPoll", (int) configuredMaxPoll.toSeconds())));
        try {
            fixture.defineTemplate(TEMPLATE);
            defineExtraFields(setting("extraFields", 100));
            insertHistory(history);
            insertBurst(rows, members);
            slowEachRowBy(setting("slowerMsPerRow", 0));

            long start = System.nanoTime();
            int polls = 0;
            while (true) {
                runner.scheduledRun();
                polls++;
                if (pending() == 0) {
                    break;
                }
                Thread.sleep(pollDelay.toMillis());
            }
            double seconds = (System.nanoTime() - start) / 1e9;

            Map<String, Object> lag = jdbc.queryForMap("""
                SELECT max(extract(epoch FROM p.processed_at - o.created_at)) AS max_lag,
                       percentile_cont(0.5) WITHIN GROUP (ORDER BY extract(epoch FROM p.processed_at - o.created_at))
                           AS median_lag
                FROM outbox o JOIN outbox_processing p ON p.outbox_id = o.outbox_id AND p.status = 'success'
                WHERE o.submission_id NOT LIKE 'history%'""", Map.of());
            System.out.printf("ETL_PEAK rows=%d members=%d batch=%d maxPollSeconds=%d pollDelaySeconds=%d "
                    + "history=%d slowerMsPerRow=%d polls=%d clearedInSeconds=%.1f medianLagSeconds=%.1f "
                    + "maxLagSeconds=%.1f%n",
                rows, members, properties.getBatchSize(), properties.getMaxPollDuration().toSeconds(),
                pollDelay.toSeconds(), history, setting("slowerMsPerRow", 0), polls, seconds,
                ((Number) lag.get("median_lag")).doubleValue(), ((Number) lag.get("max_lag")).doubleValue());
        } finally {
            properties.setBatchSize(configuredBatch);
            properties.setMaxPollDuration(configuredMaxPoll);
            slowEachRowBy(0);
        }
    }

    /** Each processed row writes one audit row; a trigger on it adds the given time inside the database. */
    private void slowEachRowBy(int millis) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.getJdbcTemplate().execute("DROP TRIGGER IF EXISTS etl_test_slow ON outbox_processing");
            jdbc.getJdbcTemplate().execute("DROP FUNCTION IF EXISTS etl_test_slow()");
            if (millis > 0) {
                jdbc.getJdbcTemplate().execute("""
                    CREATE FUNCTION etl_test_slow() RETURNS trigger LANGUAGE plpgsql AS $$
                    BEGIN PERFORM pg_sleep(%s); RETURN NEW; END $$""".formatted(millis / 1000.0));
                jdbc.getJdbcTemplate().execute("""
                    CREATE TRIGGER etl_test_slow BEFORE INSERT ON outbox_processing
                    FOR EACH ROW EXECUTE FUNCTION etl_test_slow()""");
            }
        });
    }

    /** Runs batches until {@code limit} rows are done or nothing is pending. */
    private void drain(int batch, int limit) {
        int done = 0;
        while (done < limit) {
            int pending = pending();
            if (pending == 0) {
                return;
            }
            orchestrator.runOnce("throughput", Math.min(batch, limit - done));
            done += pending - pending();
        }
    }

    private int pending() {
        return jdbc.queryForObject("SELECT count(*) FROM outbox WHERE status <> 'success'", Map.of(), Integer.class);
    }

    private void defineExtraFields(int count) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> jdbc.update("""
                INSERT INTO canonical_element (id, template_uid, preferred_name, safe_name, data_type,
                                               canonical_path, json_data_paths, created_date)
                SELECT gen_random_uuid(), :template, 'optional_' || i, 'optional_' || i, 'TEXT',
                       'optional.field_' || i, jsonb_build_array('optional.field_' || i), now()
                FROM generate_series(1, :count) AS i""",
            new MapSqlParameterSource().addValue("template", TEMPLATE).addValue("count", count)));
    }

    /** Outbox rows already processed, with payloads, as production keeps every row it ever wrote. */
    private void insertHistory(int count) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> jdbc.update("""
                INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload,
                                    event_type, status, attempt, created_at)
                SELECT 'history' || i, i, 'h' || lpad(i::text, 10, '0'), 'etlVers0001',
                       jsonb_build_object('main', jsonb_build_object('note', repeat('x', 900))),
                       'SAVE', 'success', 1, now() - interval '30 days'
                FROM generate_series(1, :count) AS i""",
            new MapSqlParameterSource().addValue("count", count)));
    }

    private void insertBurst(int count, int members) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        for (int from = 0; from < count; from += 500) {
            int to = Math.min(count, from + 500);
            int first = from;
            tx.executeWithoutResult(status -> {
                for (int i = first; i < to; i++) {
                    String uid = "bench" + String.format("%06d", i);
                    jdbc.update("""
                            INSERT INTO data_submission (id, uid, template_uid, template_version_uid,
                                                         template_version_no, assignment_uid, activity_uid,
                                                         team_uid, org_unit_uid, form_data, lock_version,
                                                         created_by, created_date, last_modified_by,
                                                         last_modified_date, deleted)
                            VALUES (:id, :uid, :template, 'etlVers0001', 1, 'etlAssign01', 'etlActiv001',
                                    'etlTeam0001', 'etlOrgUn001', CAST(:formData AS jsonb), 0, 'etl-test', now(),
                                    'etl-test', now(), false)""",
                        new MapSqlParameterSource()
                            .addValue("id", EtlFixture.submissionId(uid))
                            .addValue("uid", uid)
                            .addValue("template", TEMPLATE)
                            .addValue("formData", EtlFixture.formData(uid, members)));
                }
                jdbc.update("""
                        INSERT INTO outbox (submission_id, submission_serial_number, submission_uid, topic, payload,
                                            event_type, status, attempt, created_at)
                        SELECT id, serial_number, uid, template_version_uid, form_data, 'SAVE', 'pending', 0, now()
                        FROM data_submission
                        WHERE created_by = 'etl-test'
                          AND uid NOT IN (SELECT submission_uid FROM outbox WHERE event_type = 'SAVE')
                        ORDER BY serial_number""",
                    Map.of());
            });
        }
    }
}
