package org.nmcpye.datarun.ledger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.ledger.model.Submission;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Processes one claimed submission by reading pivot rows, mapping, and calling LedgerService.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessingWorker {

    private final NamedParameterJdbcTemplate jdbc;
    private final SubmissionMapper submissionMapper;
    private final LedgerService ledgerService;
    private final ClaimService claimService;

    private static final String READ_PIVOT_SQL =
        "SELECT * FROM pivot.fact_receipts_and_returns_9xx WHERE submission_uid = :submissionUid " +
            "AND event_type = 'repeat' ORDER BY event_id, category_uid";

    /**
     * process a single submission (assumes it is claimed by this worker).
     * On success ledgerService will mark processed; on failure, mark failure & release claim.
     */
    public void process(String submissionUid, String workerId) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(READ_PIVOT_SQL,
                new MapSqlParameterSource("submissionUid", submissionUid));
            if (rows.isEmpty()) {
                // nothing to process; mark processed to avoid repeated claims
                log.warn("Submission {} had no pivot rows; marking processed", submissionUid);
                // mark processed to avoid reattempts
                jdbc.update("UPDATE analytics.events " +
                        "SET processed_to_ledger = true, ledger_claimed_by = NULL, ledger_claimed_at = NULL " +
                        "WHERE submission_uid = :submissionUid",
                    new MapSqlParameterSource("submissionUid", submissionUid));
                return;
            }

            Submission s = submissionMapper.map(rows);
            if (s == null) {
                claimService.markFailureAndRelease(submissionUid, workerId, "mapper returned null");
                return;
            }

            // ledgerService is transactional; it will insert ledger rows and mark events processed
            ledgerService.processSubmission(s);
            log.info("Processed submission {}", submissionUid);

        } catch (Exception ex) {
            log.error("Failed to process submission {}: {}", submissionUid, ex.getMessage(), ex);
            claimService.markFailureAndRelease(submissionUid, workerId, ex.getMessage());
        }
    }
}
