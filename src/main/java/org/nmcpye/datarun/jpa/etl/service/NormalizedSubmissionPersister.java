package org.nmcpye.datarun.jpa.etl.service;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.etl.dao.IRepeatInstancesDao;
import org.nmcpye.datarun.jpa.etl.dao.ISubmissionValuesDao;
import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is transactional and does three things:
 * batch-upsert repeat instances, batch-upsert value rows, then mark-and-sweep.
 *
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@Service
@Slf4j
public class NormalizedSubmissionPersister {

    private static final int BATCH_SIZE = 500;

    private final IRepeatInstancesDao repeatDao;
    private final ISubmissionValuesDao valueDao;
    private final JdbcTemplate jdbc; // used for advisory lock

    public NormalizedSubmissionPersister(IRepeatInstancesDao repeatDao,
                                         ISubmissionValuesDao valueDao,
                                         JdbcTemplate jdbc) {
        this.repeatDao = repeatDao;
        this.valueDao = valueDao;
        this.jdbc = jdbc;
    }

    @Transactional
    public void persist(NormalizedSubmission ns) {
        long lockKey = computeAdvisoryLockKey(ns.getSubmissionId());
        // inside persist(...)
        jdbc.execute("SELECT pg_advisory_xact_lock(?)", (PreparedStatementCallback<Void>) ps -> {
            ps.setLong(1, lockKey);
            // execute() handles queries or updates — driver will return a ResultSet (or not).
            // we don't care about any returned rows/columns, just that the function ran and the lock is held.
            ps.execute();
            return null;
        });

        batchUpsertRepeatInstances(ns.getRepeatInstances());
        batchUpsertSubmissionValues(ns.getValueRows());

        // mark-and-sweep repeats
        for (Map.Entry<String, Set<String>> e : ns.getIncomingRepeatUids().entrySet()) {
            String repeatPath = e.getKey();
            Set<String> incomingSet = e.getValue();
            List<String> existing = repeatDao.findActiveRepeatUids(
                ns.getSubmissionId(), repeatPath);
            Set<String> toDelete = existing.stream().filter(uid ->
                !incomingSet.contains(uid)).collect(Collectors.toSet());
            if (!toDelete.isEmpty()) {
                repeatDao.markRepeatInstancesDeleted(ns.getSubmissionId(), repeatPath,
                    List.copyOf(toDelete));
                valueDao.markValuesDeletedForRepeatUids(ns.getSubmissionId(),
                    List.copyOf(toDelete));
            }
        }

        // mark-and-sweep multi-select selections
        for (Map.Entry<NormalizedSubmission.MultiSelectKey, Set<String>> ent :
            ns.getIncomingMultiSelects().entrySet()) {
            NormalizedSubmission.MultiSelectKey key = ent.getKey();
            Set<String> incomingIdentities = ent.getValue();

            List<String> existing = valueDao.findSelectionIdentitiesForElementRepeat(
                ns.getSubmissionId(), key.repeatInstanceId, key.elementId);

            if (incomingIdentities.isEmpty()) {
                if (!existing.isEmpty()) {
                    valueDao.markSelectionValuesDeletedByIdentity(
                        ns.getSubmissionId(), key.repeatInstanceId, key.elementId, existing);
                }
                continue;
            }

            Set<String> toDelete = existing.stream().filter(x ->
                !incomingIdentities.contains(x)).collect(Collectors.toSet());
            if (!toDelete.isEmpty()) {
                valueDao.markSelectionValuesDeletedByIdentity(ns.getSubmissionId(),
                    key.repeatInstanceId, key.elementId, List.copyOf(toDelete));
            }
        }
    }

    private void batchUpsertRepeatInstances(List<RepeatInstance> reps) {
        if (reps == null || reps.isEmpty()) return;
        int from = 0;
        while (from < reps.size()) {
            int to = Math.min(reps.size(), from + BATCH_SIZE);
            repeatDao.upsertRepeatInstancesBatch(reps.subList(from, to));
            from = to;
        }
    }

    private void batchUpsertSubmissionValues(List<ElementDataValue> rows) {
        if (rows == null || rows.isEmpty()) return;
        int from = 0;
        while (from < rows.size()) {
            int to = Math.min(rows.size(), from + BATCH_SIZE);
            valueDao.upsertSubmissionValuesBatch(rows.subList(from, to));
            from = to;
        }
    }

    private Long computeAdvisoryLockKey(String submissionId) {
        long h = 1469598103934665603L; // FNV offset basis
        for (char c : submissionId.toCharArray()) {
            h ^= c;
            h *= 1099511628211L; // FNV prime
        }
        return h;
    }
}

