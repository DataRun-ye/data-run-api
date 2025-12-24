```java
//
// EtlRun.java
package org.nmcpye.datarun.etl.entity;

@Entity
@Table(name = "etl_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtlRun {

    @Id
    @Column(name = "etl_run_id", nullable = false, updatable = false)
    private UUID etlRunId;

    @Column(name = "pipeline_name", nullable = false)
    private String pipelineName;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "status")
    private String status; // running/success/partial_fail/failed

    @Column(name = "submitted_count")
    private Long submittedCount;

    @Column(name = "success_count")
    private Long successCount;

    @Column(name = "failure_count")
    private Long failureCount;

    @Column(name = "watermark")
    private String watermark; // JSON (string) for now

    @Column(name = "metadata")
    private String metadata; // JSON (string) for now

    @Column(name = "created_by")
    private String createdBy;
}

//
// OutboxProcessing.java
package org.nmcpye.datarun.etl.entity;

@Entity
@Table(name = "outbox_processing", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"etl_run_id", "outbox_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxProcessing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "etl_run_id")
    private UUID etlRunId;

    @Column(name = "outbox_id")
    private Long outboxId;

    @Column(name = "submission_id")
    private String submissionId;

    @Column(name = "status")
    private String status; // processing/success/failure

    @Column(name = "attempt")
    private Integer attempt;

    @Column(name = "error")
    private String error;

    @Column(name = "processed_at")
    private Instant processedAt;
}

//
// TallCanonicalRow.java
package org.nmcpye.datarun.etl.model;

@Data
@Builder
public class TallCanonicalRow {
    private Long outboxId;
    private UUID ingestId;
    private String submissionId;
    private String templateUid;
    private String canonicalElementUid; // UUID string
    private String elementPath;
    private String repeatInstanceId; // nullable
    private String parentInstanceId; // nullable
    private Integer repeatIndex; // nullable
    private Integer occurrenceIndex; // fallback if repeatInstanceId absent
    private String valueText;
    private BigDecimal valueNumber;
    private String valueJson; // JSON string
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isDeleted;
}

//
// TallCanonicalJdbcRepository.java
package org.nmcpye.datarun.etl.repository;

@Repository
public class TallCanonicalJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    // Upsert targets the single unique key (instance_key, canonical_element_uid).
    private static final String UPSERT_SQL =
        "INSERT INTO tall_canonical (" +
            "instance_key, canonical_element_uid, outbox_id, ingest_id, submission_id, submission_uid, template_uid, " +
            "element_path, repeat_instance_id, parent_instance_id, repeat_index, occurrence_index, value_text, value_number, value_json, created_at, updated_at, is_deleted" +
            ") VALUES (" +
            ":instance_key, CAST(:canonical_element_uid AS uuid), :outbox_id, :ingest_id, :submission_id, :submission_uid, :template_uid, " +
            ":element_path, :repeat_instance_id, :parent_instance_id, :repeat_index, :occurrence_index, :value_text, :value_number, CAST(:value_json AS jsonb), now(), NULL, :is_deleted" +
            ") ON CONFLICT (instance_key, canonical_element_uid) DO UPDATE SET " +
            "value_text = EXCLUDED.value_text, " +
            "value_number = EXCLUDED.value_number, " +
            "value_json = EXCLUDED.value_json, " +
            "outbox_id = EXCLUDED.outbox_id, " +
            "ingest_id = EXCLUDED.ingest_id, " +
            "updated_at = now(), " +
            "is_deleted = EXCLUDED.is_deleted;";

    // truncation thresholds
    private static final int VALUE_TEXT_MAX = 4000;
    private static final int VALUE_JSON_MAX = 10000;

    public TallCanonicalJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void upsertBatch(List<TallCanonicalRow> rows) {
        if (rows == null || rows.isEmpty()) return;

        MapSqlParameterSource[] batch = new MapSqlParameterSource[rows.size()];

        for (int i = 0; i < rows.size(); i++) {
            TallCanonicalRow r = rows.get(i);
            MapSqlParameterSource p = new MapSqlParameterSource();

            // compute instance_key explicitly: COALESCE(repeat_instance_id, submission_uid)
            String instanceKey = (r.getRepeatInstanceId() != null && !r.getRepeatInstanceId().isBlank())
                ? r.getRepeatInstanceId()
                : r.getSubmissionUid();

            // sanitize values
            String valueText = r.getValueText();
            if (valueText != null && valueText.length() > VALUE_TEXT_MAX) {
                valueText = valueText.substring(0, VALUE_TEXT_MAX) + "...(truncated)";
            }

            String valueJson = r.getValueJson();
            if (valueJson != null && valueJson.length() > VALUE_JSON_MAX) {
                valueJson = valueJson.substring(0, VALUE_JSON_MAX) + "...(truncated)";
            }

            // bind parameters (null-safe)
            p.addValue("instance_key", instanceKey);
            p.addValue("canonical_element_uid", r.getCanonicalElementUid());
            p.addValue("outbox_id", r.getOutboxId());
            p.addValue("ingest_id", r.getIngestId());
            p.addValue("submission_id", r.getSubmissionId());
            p.addValue("submission_uid", r.getSubmissionUid());
            p.addValue("template_uid", r.getTemplateUid());
            p.addValue("element_path", r.getElementPath());
            p.addValue("repeat_instance_id", r.getRepeatInstanceId());
            p.addValue("parent_instance_id", r.getParentInstanceId());
            p.addValue("repeat_index", r.getRepeatIndex());
            p.addValue("occurrence_index", r.getOccurrenceIndex());
            p.addValue("value_text", valueText);
            p.addValue("value_number", r.getValueNumber());
            p.addValue("value_json", valueJson);
            p.addValue("is_deleted", Boolean.TRUE.equals(r.getIsDeleted()));

            batch[i] = p;
        }

        // execute batch update; let DataAccessException bubble to caller for orchestration to handle retries
        jdbc.batchUpdate(UPSERT_SQL, batch);
    }

    public int deleteByRepeatInstanceId(String repeatInstanceId) {
        final String sql = "DELETE FROM tall_canonical WHERE repeat_instance_id = :repeatInstanceId";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("repeatInstanceId", repeatInstanceId);
        return jdbc.update(sql, params);
    }

    public int deleteBySubmissionUid(String submissionUid) {
        final String sql = "DELETE FROM tall_canonical WHERE submission_uid = :submissionUid";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("submissionUid", submissionUid);
        return jdbc.update(sql, params);
    }
}

//
// OutboxJdbcRepository.java
package org.nmcpye.datarun.etl.repository.jdbc;

@Repository
public class OutboxJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Autowired
    public OutboxJdbcRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------
    // SQL constants
    // ---------------------
    private static final String CLAIM_SQL =
        "WITH cte AS (\n" +
            "  SELECT outbox_id\n" +
            "  FROM outbox\n" +
            "  WHERE status = 'pending'\n" +
            "    AND (next_attempt_at IS NULL OR next_attempt_at <= now())\n" +
            "  ORDER BY created_at\n" +
            "  FOR UPDATE SKIP LOCKED\n" +
            "  LIMIT :limit\n" +
            ")\n" +
            "UPDATE outbox\n" +
            "SET ingest_id = :ingestId\n" +
            "WHERE outbox_id IN (SELECT outbox_id FROM cte)\n" +
            "RETURNING outbox_id, submission_id, topic, payload, status, attempt, ingest_id, created_at;";

    private static final String MARK_SUCCESS_SQL =
        "UPDATE outbox\n" +
            "SET status = 'success', last_error = NULL, next_attempt_at = NULL\n" +
            "WHERE outbox_id = :outboxId;";

    // If you prefer the 'RETURNING' form, use this:
    private static final String MARK_SUCCESS_SQL_RETURNING =
        "UPDATE outbox\n" +
            "SET status = 'success', last_error = NULL, next_attempt_at = NULL\n" +
            "WHERE outbox_id = :outboxId\n" +
            "RETURNING outbox_id;";

    private static final String MARK_FAILURE_SQL_RETURNING =
        "UPDATE outbox\n" +
            "SET attempt = COALESCE(attempt, 0) + 1,\n" +
            "    last_error = :error,\n" +
            "    next_attempt_at = :nextAttemptAt,\n" +
            "    status = 'failed'\n" +
            "WHERE outbox_id = :outboxId\n" +
            "RETURNING outbox_id, attempt;";

    // ---------------------
    // RowMapper
    // ---------------------
    private static final RowMapper<OutboxDto> OUTBOX_ROW_MAPPER = new RowMapper<>() {
        @Override
        public OutboxDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return OutboxDto.builder()
                .outboxId(rs.getLong("outbox_id"))
                .submissionId(rs.getString("submission_id"))
                .topic(rs.getString("topic"))
                .payload(rs.getString("payload"))
                .status(rs.getString("status"))
                .attempt(rs.getInt("attempt"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null)
                .ingestId(rs.getObject("ingest_id", UUID.class))
                .build();
        }
    };

    // ---------------------
    // Public API
    // ---------------------

    @Transactional
    public List<OutboxDto> claimPendingBatch(UUID ingestId, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("ingestId", ingestId)
            .addValue("limit", limit);

        // Execute the UPDATE ... RETURNING and map the returned rows to DTOs
        List<OutboxDto> claimed = jdbc.query(CLAIM_SQL, params, OUTBOX_ROW_MAPPER);
        if (claimed == null) {
           return Collections.emptyList();
        }
        return claimed;
    }

    @Transactional
    public void markOutboxSuccess(long outboxId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("outboxId", outboxId);

        // prefer simple update; check updated row count via jdbc.update
        int updated = jdbc.update(MARK_SUCCESS_SQL, params);
        if (updated == 0) {
            // Could be that row doesn't exist or was concurrently modified; caller should handle.
            throw new IllegalStateException("markOutboxSuccess: no outbox row updated for id=" + outboxId);
        }
    }

    @Transactional
    public Optional<Integer> markOutboxFailure(long outboxId, String error, Instant nextAttemptAt) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("outboxId", outboxId)
            .addValue("error", error)
            .addValue("nextAttemptAt", nextAttemptAt);

        // Use RETURNING to obtain the updated attempt count
        List<Map<String, Object>> rows = jdbc.queryForList(MARK_FAILURE_SQL_RETURNING, params);
        if (rows.isEmpty()) {
            // no row updated -> signal the caller
            throw new IllegalStateException("markOutboxFailure: no outbox row updated for id=" + outboxId);
        }
        Object attemptObj = rows.get(0).get("attempt");
        if (attemptObj == null) return Optional.empty();

        int attempt;
        if (attemptObj instanceof Number) {
            attempt = ((Number) attemptObj).intValue();
        } else {
            attempt = Integer.parseInt(attemptObj.toString());
        }
        return Optional.of(attempt);
    }

    @Transactional
    public boolean markOutboxSuccessReturning(long outboxId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("outboxId", outboxId);
        List<Long> ids = jdbc.query(MARK_SUCCESS_SQL_RETURNING, params, (rs, rowNum) -> rs.getLong("outbox_id"));
        return !ids.isEmpty();
    }
}

//
// OutboxClaimServiceImpl.java
package org.nmcpye.datarun.etl.service.impl;

@Service
public class OutboxClaimServiceImpl implements OutboxClaimService {

    private static final Logger log = LoggerFactory.getLogger(OutboxClaimServiceImpl.class);

    private final OutboxJdbcRepository outboxJdbcRepository;

    public OutboxClaimServiceImpl(OutboxJdbcRepository outboxJdbcRepository) {
        this.outboxJdbcRepository = outboxJdbcRepository;
    }

    @Override
    public List<OutboxDto> claimBatch(EtlRun run, int batchSize) {
        UUID ingestId = UUID.randomUUID();
        // We intentionally do not persist the ingestId into EtlRun here; caller/ orchestrator manages run metadata.
        return claimBatchWithIngestId(ingestId, batchSize);
    }

    @Override
    public List<OutboxDto> claimBatchWithIngestId(UUID ingestId, int batchSize) {
        log.debug("Attempting to claim up to {} outbox rows with ingestId={}", batchSize, ingestId);
        List<OutboxDto> claimed = outboxJdbcRepository.claimPendingBatch(ingestId, batchSize);
        int n = (claimed == null) ? 0 : claimed.size();
        log.debug("Claimed {} outbox rows with ingestId={}", n, ingestId);
        return (claimed == null) ? List.of() : claimed;
    }
}

//
// OutboxProcessingServiceImpl.java
package org.nmcpye.datarun.etl.service.impl;

@Service
public class OutboxProcessingServiceImpl implements OutboxProcessingService {

    private final OutboxProcessingRepository outboxProcessingRepository;
    private final OutboxJdbcRepository outboxJdbcRepository;
    private final EtlRunService etlRunService;

    public OutboxProcessingServiceImpl(OutboxProcessingRepository outboxProcessingRepository,
                                       OutboxJdbcRepository outboxJdbcRepository,
                                       EtlRunService etlRunService) {
        this.outboxProcessingRepository = outboxProcessingRepository;
        this.outboxJdbcRepository = outboxJdbcRepository;
        this.etlRunService = etlRunService;
    }

    @Override
    @Transactional
    public OutboxProcessing startProcessing(UUID etlRunId, OutboxDto outbox) {
        // TODO: insert an outbox_processing row with status 'processing'
        OutboxProcessing op = OutboxProcessing.builder()
            .etlRunId(etlRunId)
            .outboxId(outbox.getOutboxId())
            .submissionId(outbox.getSubmissionId())
            .status("processing")
            .attempt(outbox.getAttempt())
            .processedAt(Instant.now())
            .build();

        return outboxProcessingRepository.persist(op);
    }

    @Override
    @Transactional
    public void recordSuccess(UUID etlRunId, OutboxDto outbox) {
        // quick existence check via repository (uses findByEtlRunIdAndStatus from skeleton)
        List<OutboxProcessing> existingSuccess = outboxProcessingRepository.findByEtlRunIdAndStatus(etlRunId, "success");
        boolean alreadyRecorded = existingSuccess.stream()
            .anyMatch(p -> Objects.equals(p.getOutboxId(), outbox.getOutboxId()));

        if (alreadyRecorded) {
            // already recorded success for this run+outbox -> idempotent no-op
            return;
        }

        // create success processing record (will be rolled back if subsequent steps fail)
        OutboxProcessing successRecord = OutboxProcessing.builder()
            .etlRunId(etlRunId)
            .outboxId(outbox.getOutboxId())
            .submissionId(outbox.getSubmissionId())
            .status("success")
            .attempt(outbox.getAttempt())
            .error(null)
            .processedAt(Instant.now())
            .build();

        try {
            outboxProcessingRepository.persist(successRecord);
        } catch (DataIntegrityViolationException dive) {
            // concurrent insertion by another worker: treat as already recorded and continue
            // unique constraint on (etl_run_id, outbox_id) ensures only one success row per run/outbox
        }

        // update outbox status to 'success' (joins same transaction)
        outboxJdbcRepository.markOutboxSuccess(outbox.getOutboxId());

        // increment run-level success counter
        etlRunService.incrementSuccess(etlRunId, 1);
    }

    @Override
    @Transactional
    public void recordFailure(UUID etlRunId, OutboxDto outbox, String error, Instant nextAttemptAt) {
        // sanitize / truncate error message to avoid storing huge stack traces (e.g., cap at 1000 chars)
        final int ERROR_MAX_LEN = 1000;
        String safeError = (error == null) ? "unspecified error" : error;
        if (safeError.length() > ERROR_MAX_LEN) {
            safeError = safeError.substring(0, ERROR_MAX_LEN) + "...(truncated)";
        }

        // insert failure processing record (will be rolled back if markOutboxFailure fails)
        OutboxProcessing failureRecord = OutboxProcessing.builder()
            .etlRunId(etlRunId)
            .outboxId(outbox.getOutboxId())
            .submissionId(outbox.getSubmissionId())
            .status("failure")
            .attempt(outbox.getAttempt())
            .error(safeError)
            .processedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS))
            .build();

        try {
            outboxProcessingRepository.persist(failureRecord);
        } catch (DataIntegrityViolationException dive) {
            // concurrent insertion (same (etl_run_id, outbox_id)) - proceed to ensure outbox row is updated.
            // The unique constraint ensures only one bookkeeping row per run+outbox; duplicate attempts are safe.
        }

        // call JDBC repo to update outbox attempt/last_error/next_attempt_at and capture the new attempt count
        Optional<Integer> attemptOpt = outboxJdbcRepository.markOutboxFailure(outbox.getOutboxId(), safeError, nextAttemptAt);

        if (attemptOpt.isEmpty()) {
            // markOutboxFailure did not update any row -> inconsistent state; surface to caller for retry handling
            throw new IllegalStateException("markOutboxFailure did not affect outbox row for id=" + outbox.getOutboxId());
        }

        // increment run-level failure counter
        etlRunService.incrementFailure(etlRunId, 1);
    }
}


//
// EtlRunServiceImpl.java
package org.nmcpye.datarun.etl.service.impl;

@Service
public class EtlRunServiceImpl implements EtlRunService {

    private static final String INCREMENT_SUCCESS_SQL =
            "UPDATE etl_runs SET success_count = COALESCE(success_count, 0) + :delta WHERE etl_run_id = :id";

    private static final String INCREMENT_FAILURE_SQL =
            "UPDATE etl_runs SET failure_count = COALESCE(failure_count, 0) + :delta WHERE etl_run_id = :id";

    private final EtlRunRepository etlRunRepository;
    private final NamedParameterJdbcTemplate jdbc;

    public EtlRunServiceImpl(EtlRunRepository etlRunRepository,
                             NamedParameterJdbcTemplate jdbc) {
        this.etlRunRepository = etlRunRepository;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public EtlRun startRun(String pipelineName, String createdBy, String codeVersion) {
        EtlRun run = EtlRun.builder()
                .etlRunId(UUID.randomUUID())
                .pipelineName(pipelineName)
                .startedAt(Instant.now())
                .status("running")
                .submittedCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .createdBy(createdBy)
                .metadata("{\"codeVersion\":\"" + codeVersion + "\"}")
                .build();

        return etlRunRepository.persist(run);
    }

    @Override
    @Transactional
    public EtlRun finishRun(UUID etlRunId, String status, long submittedCount, long successCount, long failureCount) {
        EtlRun run = etlRunRepository.findById(etlRunId)
                .orElseThrow(() -> new IllegalArgumentException("etl run not found: " + etlRunId));
        run.setFinishedAt(Instant.now());
        run.setStatus(status);
        run.setSubmittedCount(submittedCount);
        run.setSuccessCount(successCount);
        run.setFailureCount(failureCount);
        return etlRunRepository.persist(run);
    }

    @Override
    @Transactional
    public void incrementSuccess(UUID etlRunId, int delta) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("delta", delta)
                .addValue("id", etlRunId);
        int updated = jdbc.update(INCREMENT_SUCCESS_SQL, params);
        if (updated == 0) {
            // No row updated: caller might have passed an invalid etlRunId; surface as IllegalStateException
            throw new IllegalStateException("incrementSuccess: no etl_runs row found for id=" + etlRunId);
        }
    }

    @Override
    @Transactional
    public void incrementFailure(UUID etlRunId, int delta) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("delta", delta)
                .addValue("id", etlRunId);
        int updated = jdbc.update(INCREMENT_FAILURE_SQL, params);
        if (updated == 0) {
            // No row updated: caller might have passed an invalid etlRunId; surface as IllegalStateException
            throw new IllegalStateException("incrementFailure: no etl_runs row found for id=" + etlRunId);
        }
    }
}


//
// EtlOrchestrator.java
package org.nmcpye.datarun.etl.orchestrator;

@Component
public class EtlOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(EtlOrchestrator.class);

    private final EtlRunService etlRunService;
    private final OutboxClaimService outboxClaimService;
    private final OutboxProcessingService outboxProcessingService;
    private final TransformService transformService;
    private final TransactionTemplate tx;

    public EtlOrchestrator(EtlRunService etlRunService,
                           OutboxClaimService outboxClaimService,
                           OutboxProcessingService outboxProcessingService,
                           TransformService transformService,
                           PlatformTransactionManager txManager) {
        this.etlRunService = etlRunService;
        this.outboxClaimService = outboxClaimService;
        this.outboxProcessingService = outboxProcessingService;
        this.transformService = transformService;
        this.tx = new TransactionTemplate(txManager);
        // keep short transaction timeout/default settings; can be tuned later
    }

    public void runOnce(String pipelineName, int batchSize) {
        // derive runtime metadata
        String createdBy = "etl-orchestrator";
        String codeVersion = System.getenv().getOrDefault("CODE_VERSION", "unknown");

        // 1) start run (persist a running etl_runs record)
        EtlRun run = etlRunService.startRun(pipelineName, createdBy, codeVersion);
        UUID runId = run.getEtlRunId();
        log.info("Started ETL run {} for pipeline='{}' (code={})", runId, pipelineName, codeVersion);

        // generate ingestId used to tag claimed outbox rows
        UUID ingestId = UUID.randomUUID();

        // counters
        int submittedCount = 0;
        int successCount = 0;
        int failureCount = 0;

        try {
            // 2) claim a batch of pending outbox rows and assign them ingestId
            List<OutboxDto> claimed = outboxClaimService.claimBatchWithIngestId(ingestId, batchSize);
            submittedCount = (claimed == null) ? 0 : claimed.size();
            log.info("Run {} claimed {} outbox rows (ingestId={})", runId, submittedCount, ingestId);

            // 3) process each claimed outbox row
            if (claimed != null) {
                for (OutboxDto outbox : claimed) {
                    try {
                        // per-outbox transaction: startProcessing + transform + recordSuccess
                        tx.execute(status -> {
                            // startProcessing inserts a 'processing' audit row (joins this TX)
                            outboxProcessingService.startProcessing(runId, outbox);

                            // transformAndPersist is currently a placeholder; when implemented it must be idempotent
                            try {
                                transformService.transformAndPersist(outbox, ingestId);
                            } catch (Exception e) {
                                // rethrow to trigger TX rollback so we do not persist a success record
                                throw new RuntimeException(e);
                            }

                            // on successful transform, record success (joins same TX and will be committed)
                            outboxProcessingService.recordSuccess(runId, outbox);

                            return null;
                        });

                        successCount++;
                    } catch (Exception ex) {
                        // transform failed or tx failed: persist failure bookkeeping in its own transaction.
                        // compute a simple nextAttemptAt (e.g., 60s backoff). Real backoff computed by caller later.
                        Instant nextAttemptAt = Instant.now().plusSeconds(60);

                        String shortError = (ex.getMessage() == null) ? ex.getClass().getSimpleName() : ex.getMessage();
                        // recordFailure is transactional and will persist the failure row and update outbox attempts
                        outboxProcessingService.recordFailure(runId, outbox, shortError, nextAttemptAt);

                        failureCount++;
                        log.warn("Processing failed for outboxId={} in run {}: {}", outbox.getOutboxId(), runId, shortError);
                    }
                }
            }

            // 4) finish run - mark finished and persist counters
            etlRunService.finishRun(runId, "finished", submittedCount, successCount, failureCount);
            log.info("Finished ETL run {}: submitted={}, success={}, failure={}", runId, submittedCount, successCount, failureCount);

        } catch (Exception topEx) {
            // If something unexpected happens at the orchestration level, attempt to mark the run as failed.
            try {
                etlRunService.finishRun(runId, "failed", submittedCount, successCount, failureCount);
            } catch (Exception e) {
                log.error("Failed to update etl_runs status after orchestration error for run {}", runId, e);
            }
            log.error("Orchestration error for run {}: {}", runId, topEx.getMessage(), topEx);
            // rethrow or swallow depending on upstream requirements; here we rethrow to surface the failure
            throw new RuntimeException(topEx);
        }
    }

    void processSingle(EtlRun run, OutboxDto outbox) {
        throw new UnsupportedOperationException("processSingle not implemented");
    }
}

//
// EventProcessorServiceImpl.java
package org.nmcpye.datarun.etl.service.impl;

@Service
public class EventProcessorServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(EventProcessorServiceImpl.class);

    private final TransformService transformService;
    private final TallCanonicalJdbcRepository tallRepo;
    private final TemplateElementRepository templateElementRepository;
    private final OutboxProcessingService outboxProcessingService;
    private final OutboxJdbcRepository outboxJdbcRepository;
    private final EtlRunService etlRunService;
    private final PlatformTransactionManager txManager;

    public EventProcessorServiceImpl(TransformService transformService,
                                     TallCanonicalJdbcRepository tallRepo,
                                     TemplateElementRepository templateElementRepository,
                                     OutboxProcessingService outboxProcessingService,
                                     OutboxJdbcRepository outboxJdbcRepository,
                                     EtlRunService etlRunService,
                                     PlatformTransactionManager txManager) {
        this.transformService = transformService;
        this.tallRepo = tallRepo;
        this.templateElementRepository = templateElementRepository;
        this.outboxProcessingService = outboxProcessingService;
        this.outboxJdbcRepository = outboxJdbcRepository;
        this.etlRunService = etlRunService;
        this.txManager = txManager;
    }

    public void processEvent(UUID etlRunId, OutboxDto outbox, UUID ingestId) throws Exception {
        if (outbox == null) return;

        final Long outboxId = outbox.getOutboxId();
        final String eventType = outbox.getEventType() == null ? "SAVE" : outbox.getEventType().toUpperCase();

        try {
            if ("DELETE".equals(eventType)) {
                // DELETE semantics: hard-delete rows for repeat instance or submission
                if (outbox.getRepeatInstanceId() != null && !outbox.getRepeatInstanceId().isBlank()) {
                    int deleted = tallRepo.deleteByRepeatInstanceId(outbox.getRepeatInstanceId());
                    log.debug("Deleted {} tall rows for repeat_instance_id={} (outboxId={})",
                        deleted, outbox.getRepeatInstanceId(), outboxId);
                } else {
                    int deleted = tallRepo.deleteBySubmissionUid(outbox.getSubmissionUid());
                    log.debug("Deleted {} tall rows for submission_uid={} (outboxId={})",
                        deleted, outbox.getSubmissionUid(), outboxId);
                }

                // record success (recordSuccess will update outbox.status and etl run counters)
                outboxProcessingService.recordSuccess(etlRunId, outbox);
                // outboxProcessingService.recordSuccess handles markOutboxSuccess and etlRunService.incrementSuccess
                return;
            }

            // SAVE / UPDATE / REPLACE / BACKFILL -> mapping + upsert
            List<TallCanonicalRow> rows = transformService.transform(outbox);

            if (rows == null || rows.isEmpty()) {
                // nothing to persist -> still a successful processing
                outboxProcessingService.recordSuccess(etlRunId, outbox);
                return;
            }

            // ensure provenance fields filled before persisting
            for (TallCanonicalRow r : rows) {
                if (r.getIngestId() == null) r.setIngestId(ingestId);
                if (r.getOutboxId() == null) r.setOutboxId(outboxId);
                if (r.getSubmissionId() == null) r.setSubmissionId(outbox.getSubmissionId());
                if (r.getSubmissionUid() == null) r.setSubmissionUid(outbox.getSubmissionUid());
            }

            // idempotent upsert: unique constraint on instance_key + canonical_element_uid prevents duplicates
            tallRepo.upsertBatch(rows);

            // on success, record success (updates outbox and run counters)
            outboxProcessingService.recordSuccess(etlRunId, outbox);
        } catch (Exception ex) {
            // on any transient/DB/transform error, persist failure in a separate REQUIRES_NEW TX so it survives outer rollback
            String shortError = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            if (shortError.length() > 1000) shortError = shortError.substring(0, 1000) + "...(truncated)";

            Instant nextAttemptAt = Instant.now().plusSeconds(60); // simple backoff; orchestrator can later use smarter logic

            // recordFailure in a new transaction so failure record and outbox retry scheduling are durable
            recordFailureRequiresNewTx(etlRunId, outbox, shortError, nextAttemptAt);

            // rethrow to let orchestrator perform outer rollback / higher-level handling
            throw ex;
        }
    }

    /**
     * Persist failure details in a new, independent transaction so the failure audit and retry scheduling
     * are durable even if the calling transaction rolls back.
     * <p>
     * We use TransactionTemplate with PROPAGATION_REQUIRES_NEW to suspend the calling tx and start a fresh one.
     */
    private void recordFailureRequiresNewTx(UUID etlRunId, OutboxDto outbox, String shortError, Instant nextAttemptAt) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionTemplate txTemplate = new TransactionTemplate(txManager, def);
        txTemplate.execute(status -> {
            // delegate to existing OutboxProcessingService which inserts a failure record and updates outbox attempt info
            outboxProcessingService.recordFailure(etlRunId, outbox, shortError, nextAttemptAt);
            // note: outboxProcessingService.recordFailure will call OutboxJdbcRepository.markOutboxFailure and EtlRunService.incrementFailure
            return null;
        });
    }
}

//
// TransformServiceImpl.java
package org.nmcpye.datarun.etl.service.impl;

@Service
@RequiredArgsConstructor
public class TransformServiceImpl implements TransformService {

    private static final Logger log = LoggerFactory.getLogger(TransformServiceImpl.class);
    private static final int VALUE_TEXT_MAX = 4000;

    private final TemplateElementRepository templateElementRepository;
    private final DataSubmissionRepository submissionRepository;

    private final ObjectMapper objectMapper;

    public List<TallCanonicalRow> transform(OutboxDto outbox) {
        
    }
}
```

