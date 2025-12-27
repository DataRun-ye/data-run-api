package org.nmcpye.datarun.outbox.migrationbackfill;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * One-time migration runner that inserts backfill outbox rows for existing submissions.
 * <p>
 * Usage:
 * - Run with a dedicated Spring profile or run once and remove.
 * - Configure batchSize via constructor or environment variable if desired.
 * <p>
 * Behavior:
 * - Reads data_submission rows (id, payload, submission_uid, formVersion) in batches ordered by created_at
 * - Skips submissions which already have an outbox row (idempotent)
 * - Inserts one outbox row per submission with event_type='BACKFILL', payload set to existing submission JSON,
 * submission_uid, and template_version_uid populated from formVersion (if present)
 */
//@Component
public class OneTimeOutboxBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OneTimeOutboxBackfillRunner.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final int batchSize = 500; // tune as needed

    @Value("${migration.runOutboxBackfill:false}")
    private boolean enabled;

    @Value("${migration.submissionUids:}")
    private String submissionUidsCsv;

    public OneTimeOutboxBackfillRunner(NamedParameterJdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Safety: only run if explicitly enabled (env flag) to avoid accidental runs in prod.
        if (!enabled) {
            log.info("OneTimeOutboxBackfillRunner disabled (set RUN_OUTBOX_BACKFILL=true to enable). Exiting.");
            return;
        }

        log.info("Starting one-time outbox backfill migration (batchSize={})", batchSize);

        // Count total submissions to process
        Integer total = jdbc.queryForObject("SELECT COUNT(1) FROM data_submission", new MapSqlParameterSource(), Integer.class);
        if (total == null || total == 0) {
            log.info("No submissions found. Exiting.");
            return;
        }
        log.info("Total submissions to scan: {}", total);


        long offset = 0;
        int processed = 0;
        List<String> restrictUids = parseCsv(submissionUidsCsv);
        while (true) {
            // Fetch a batch of submissions: adjust columns to match your data_submission schema
            String selectSql = "SELECT id, uid, template_version_uid, form_data, serial_number " +
                "FROM data_submission " +
                "WHERE data_submission.template_uid in (:uids) " +
                "ORDER BY serial_number ASC " +
                "LIMIT :limit OFFSET :offset";

            MapSqlParameterSource selectParams = new MapSqlParameterSource()
                .addValue("limit", batchSize)
                .addValue("uids", restrictUids)
                .addValue("offset", offset);

            List<Map<String, Object>> submissions = jdbc.queryForList(selectSql, selectParams);
            if (submissions.isEmpty()) break;

            List<MapSqlParameterSource> inserts = new ArrayList<>(submissions.size());
            for (Map<String, Object> row : submissions) {
                String submissionId = Objects.toString(row.get("id"), null);
                String submissionUid = Objects.toString(row.get("uid"), null);
                String templateVersionUid = Objects.toString(row.get("form_data"), null);
                Long submissionSerial = Long.parseLong(Objects.toString(row.get("serial_number"), null));
                String payload = Objects.toString(row.get("form_data"), null);

                if (submissionId == null) continue;

                // Idempotency check: skip if outbox already contains this submission_id
                Integer existing = jdbc.queryForObject(
                    "SELECT COUNT(1) FROM outbox WHERE submission_id = :submissionId",
                    new MapSqlParameterSource("submissionId", submissionId),
                    Integer.class
                );
                if (existing != null && existing > 0) {
                    log.debug("Skipping submission {} - outbox row already exists", submissionId);
                    processed++;
                    continue;
                }

                // Build insert param map for outbox
                MapSqlParameterSource p = new MapSqlParameterSource();
                p.addValue("submission_serial_number", submissionSerial);
                p.addValue("submission_id", submissionId);
                p.addValue("submission_uid", submissionUid);
                p.addValue("topic", templateVersionUid); // topic optional; you may choose to set differently
                p.addValue("payload", payload);
                p.addValue("status", "pending");
                p.addValue("attempt", 0);
                p.addValue("last_error", null);
                p.addValue("next_attempt_at", null);
                p.addValue("ingest_id", null);
                p.addValue("created_at", row.get("created_at"));
                p.addValue("event_type", "BACKFILL");

                inserts.add(p);
            }

            if (!inserts.isEmpty()) {
                // Insert batch; adapt the SQL to match your outbox table column names exactly.
                String insertSql = "INSERT INTO outbox (" +
                    "submission_serial_number, submission_id, submission_uid, topic, payload, status, attempt, last_error, next_attempt_at, ingest_id, created_at, event_type" +
                    ") VALUES (" +
                    ":submission_serial_number, :submission_id, :submission_uid, :topic, cast(:payload AS jsonb), :status, :attempt, :last_error, :next_attempt_at, :ingest_id, :created_at, :event_type" +
                    ") ON CONFLICT (submission_id) WHERE (event_type = 'BACKFILL') DO NOTHING";

                try {
                    MapSqlParameterSource[] batchParams = inserts.toArray(new MapSqlParameterSource[0]);
                    int[] results = jdbc.batchUpdate(insertSql, batchParams);
                    int inserted = 0;
                    for (int r : results) if (r >= 0) inserted++;
                    log.info("Inserted {} outbox rows for this batch (offset={})", inserted, offset);
                } catch (DataAccessException dae) {
                    log.error("Batch insert failed at offset {}: {}", offset, dae.getMessage());
                    throw dae; // let caller handle; this run should stop so you can fix issues
                }
            } else {
                log.info("No new outbox rows to insert in this batch (offset={})", offset);
            }

            processed += submissions.size();
            offset += submissions.size();

            // optional: break early for testing
            // if (offset > 2000) break;
        }

        log.info("One-time outbox backfill completed. Scanned {}, inserted into outbox where missing.", offset);
    }

    private List<String> parseCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
}
