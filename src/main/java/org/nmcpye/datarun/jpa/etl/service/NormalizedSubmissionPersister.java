package org.nmcpye.datarun.jpa.etl.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.etl.dao.IRepeatInstancesDao;
import org.nmcpye.datarun.jpa.etl.dao.ISubmissionValuesDao;
import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@Service
@RequiredArgsConstructor
public class NormalizedSubmissionPersister {

    private final IRepeatInstancesDao repeatInstancesDao;
    private final ISubmissionValuesDao submissionValuesDao;

    /**
     * For the advisory lock
     */
    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Does the following:
     * <ul>
     * <ol>1. Acquire per-submission advisory lock through {@link #acquireAdvisoryLock(String)}</ol>
     * <ol>2. HOLISTIC PURGE: Mark all existing data for this submission as deleted
     * {@link ISubmissionValuesDao#markAllAsDeletedForSubmission(String)},
     * {@link IRepeatInstancesDao#markAllAsDeletedForSubmission(String)}.
     *  <pre>(The order here doesn't strictly matter due to the transaction, but purging values first is logical.)</pre></ol>
     * <ol>3. UPSERT NEW STATE: Insert the new data. The UPSERT logic will "undelete" any rows that
     * existed before and are still present in the new submission.
     *      <pre> (Process repeats first {@link IRepeatInstancesDao#upsertRepeatInstancesBatch(List)} so value table's foreign key (if any) is valid..)</pre></ol>
     * <ol>4. Lock is released automatically when the transaction commits.</ol>
     * </ul>
     *
     * @param ns the normalized submission to upsert. usually incoming from the normalization stage
     * @see EtlCoordinatorService
     * @see Normalizer
     */
    @Transactional
    public void persist(NormalizedSubmission ns) {
        final String submissionId = ns.getSubmissionId();

        // 1. Acquire per-submission advisory lock
        acquireAdvisoryLock(submissionId);

        // 2. HOLISTIC PURGE: Mark all existing data for this submission as deleted.
        submissionValuesDao.markAllAsDeletedForSubmission(submissionId);
        repeatInstancesDao.markAllAsDeletedForSubmission(submissionId);

        // 3. UPSERT NEW STATE: Insert the new data. The UPSERT logic will "undelete" any
        if (!ns.getRepeatInstances().isEmpty()) {
            repeatInstancesDao.upsertRepeatInstancesBatch(ns.getRepeatInstances());
        }
        if (!ns.getValues().isEmpty()) {
            submissionValuesDao.upsertSubmissionValuesBatch(ns.getValues());
        }

        // 4. Lock is released automatically when the transaction commits.
    }

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
     * Deterministic 64-bit value derived from the SHA-256 of the input string.
     * We take the first 8 bytes of the digest as a long (big-endian).
     * This is portable (no DB extension) and has very low collision probability.
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
