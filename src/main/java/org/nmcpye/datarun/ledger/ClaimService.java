package org.nmcpye.datarun.ledger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ClaimService: claim a batch of unprocessed submissions for processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimService {

    private final NamedParameterJdbcTemplate jdbc;

    // claim SQL as explained above; note variable :claimStaleMinutes get substituted as string in SQL - we can param bind it too
    private static final String CLAIM_SQL =
        "WITH candidates AS ( " +
            "  SELECT submission_uid " +
            "  FROM analytics.events " +
            "   JOIN analytics.config_template_mapping ctm on events.template_uid = ctm.template_uid" +
            "  WHERE processed_to_ledger = false " +
            "    AND (ledger_claimed_by IS NULL OR ledger_claimed_at < now() - interval :claimStaleMinutes) " +
            "  ORDER BY submission_creation_time " +
            "  FOR UPDATE SKIP LOCKED " +
            "  LIMIT :limit " +
            ") " +
            "UPDATE analytics.events e " +
            "SET ledger_claimed_by = :workerId, ledger_claimed_at = now() " +
            "FROM candidates c " +
            "WHERE e.submission_uid = c.submission_uid " +
            "RETURNING e.submission_uid, e.submission_serial";

    /**
     * Claim up to `limit` submissions. Returns claimed submission_uids.
     *
     * @param workerId           unique id for this worker instance
     * @param limit              how many to claim
     * @param claimStaleInterval interval literal like '30 minutes' or '5 minutes' — see usage below
     */
    public List<String> claimBatch(String workerId, int limit, String claimStaleInterval) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("workerId", workerId)
            .addValue("limit", limit)
            .addValue("claimStaleMinutes", claimStaleInterval); // pass string '30 minutes' or '5 minutes'

        // NamedParameterJdbcTemplate doesn't allow interval param directly substituted; we will inline by building SQL
        String inlinedSql = CLAIM_SQL.replace(":claimStaleMinutes", "'" + claimStaleInterval + "'");
        List<Map<String, Object>> rows = jdbc.queryForList(inlinedSql, params);
        List<String> uids = rows.stream().map(
            r -> (String) r.get("submission_uid")).collect(Collectors.toList());
        log.info("Worker {} claimed {} submissions", workerId, uids.size());
        return uids;
    }

    /**
     * Release a claim (clear claim fields) for a given submission (called on permanent failure or manual reset).
     */
    public void releaseClaim(String submissionUid) {
        MapSqlParameterSource p = new MapSqlParameterSource("submissionUid", submissionUid);
        jdbc.update("UPDATE analytics.events SET ledger_claimed_by = NULL, ledger_claimed_at = NULL " +
            "WHERE submission_uid = :submissionUid", p);
    }

    /**
     * Mark failure: increment failure count and clear claim so others can retry later.
     */
    public void markFailureAndRelease(String submissionUid, String workerId, String errorMessage) {
        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("submissionUid", submissionUid)
            .addValue("workerId", workerId)
            .addValue("errorMessage", errorMessage);
        jdbc.update("UPDATE analytics.events SET ledger_failures_count = coalesce(ledger_failures_count,0) + 1, " +
            "last_ledger_failure = now(), ledger_claimed_by = NULL, ledger_claimed_at = NULL " +
            "WHERE submission_uid = :submissionUid", p);
        // record issue
        MapSqlParameterSource pi = new MapSqlParameterSource()
            .addValue("submissionUid", submissionUid)
            .addValue("workerId", workerId)
            .addValue("errorMessage", errorMessage)
            .addValue("details", null);
        jdbc.update("INSERT INTO analytics.processing_issues (submission_uid, worker_id, error_message) " +
            "VALUES (:submissionUid, :workerId, :errorMessage)", pi);
    }
}
