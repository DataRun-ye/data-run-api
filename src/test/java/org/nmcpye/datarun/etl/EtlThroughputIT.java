package org.nmcpye.datarun.etl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.etl.EtlFixture.TEMPLATE;

/**
 * Measures how fast the ETL drains a burst of outbox rows, on synthetic submissions. Off by default; run with
 * {@code -Detl.throughput=true}, and optionally {@code -Detl.throughput.rows}, {@code .members} (repeat rows
 * per submission), {@code .extraFields} (field definitions absent from the payload, as optional fields are),
 * {@code .batch} and {@code .history} (already processed outbox rows, which the claim reads past).
 * Prints one {@code ETL_THROUGHPUT} line with counts and timings only.
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
