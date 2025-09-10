package org.nmcpye.datarun.jpa.etl.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.etl.dao.IElementDataValueDao;
import org.nmcpye.datarun.jpa.etl.dao.IRepeatInstancesDao;
import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Persist a fully-normalized submission into the database using DAOs.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Acquire a per-submission advisory lock (transaction-scoped) to prevent concurrent ETL runs for the same submission.</li>
 *   <li>Holistic purge: mark existing repeat instances and value rows for the submission as deleted.</li>
 *   <li>Upsert new state: write repeat instances first (to satisfy FK constraints), then element value rows.</li>
 *   <li>Let the transaction commit/rollback handle release of the advisory lock and DB consistency.</li>
 * </ul>
 *
 * <p>Design notes:
 * <ul>
 *   <li>Uses JDBC/NamedParameterJdbcTemplate only for acquiring advisory lock (pg_advisory_xact_lock).</li>
 *   <li>Deterministic 64-bit lock key derived from SHA-256 of submissionId for portability and low collision risk.</li>
 * </ul>
 *
 * @author Hamza Assada
 * @since 13/08/2025
 */
@Service
@RequiredArgsConstructor
public class NormalizedSubmissionPersister {

    private final IRepeatInstancesDao repeatInstancesDao;
    private final IElementDataValueDao submissionValuesDao;

    /**
     * For the advisory lock
     */
    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Persist the provided NormalizedSubmission.
     *
     * <ol>
     *   <li>Acquire advisory lock for submissionId</li>
     *   <li>Mark existing data as deleted for the submission (values, repeats)</li>
     *   <li>Upsert repeat instances (if any)</li>
     *   <li>Upsert element value rows (if any)</li>
     * </ol>
     * <p>
     * The method is transaction-scoped; lock is released when the transaction ends.
     *
     * @param ns normalized submission to persist
     */
    @Transactional
    public void persist(NormalizedSubmission ns) {
        final String submissionId = ns.getSubmissionUid();

        // Acquire per-submission advisory lock
        acquireAdvisoryLock(submissionId);

        // HOLISTIC PURGE: Mark all existing data for this submission as deleted.
        submissionValuesDao.markAllAsDeletedForSubmission(submissionId);
        repeatInstancesDao.markAllAsDeletedForSubmission(submissionId);

        // UPSERT NEW STATE: Insert the new data. The UPSERT logic will "undelete" any
        if (!ns.getRepeatInstances().isEmpty()) {
            repeatInstancesDao.upsertRepeatInstancesBatch(ns.getRepeatInstances());
        }
        if (!ns.getValues().isEmpty()) {
            submissionValuesDao.upsertSubmissionValuesBatch(ns.getValues());
        }

        // Lock is released automatically when the transaction commits.
    }

    /**
     * Blocking, transaction-scoped advisory lock for a submission.
     * Uses pg_advisory_xact_lock(:key).
     *
     * @param submissionId submission identifier
     */
    private void acquireAdvisoryLock(String submissionId) {
        long key = stableLongFromString(submissionId);
        String sql = "SELECT pg_advisory_xact_lock(:key)"; // blocking, transaction-scoped lock
        jdbcTemplate.query(sql, new MapSqlParameterSource("key", key), rs -> {
            // consume the ResultSet (the function returns a single row with a single column
            // that some drivers expose as a value; we don't need it — just iterate)
            while (rs.next()) { /* no-op */ }
            return null; // ResultSetExtractor requires a return
        });
    }

    /**
     * Deterministic 64-bit long derived from SHA-256(submissionId).
     * Returns the first 8 bytes as a big-endian long.
     *
     * @param s input string (submissionId)
     * @return 64-bit long key
     */
    private static long stableLongFromString(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            ByteBuffer bb = ByteBuffer.wrap(digest, 0, Long.BYTES);
            return bb.getLong();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always present; rethrow as unchecked if somehow missing
            throw new IllegalStateException("SHA-256 MessageDigest not available", e);
        }
    }
}
