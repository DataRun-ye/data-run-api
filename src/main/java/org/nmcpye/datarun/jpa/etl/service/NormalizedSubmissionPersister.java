package org.nmcpye.datarun.jpa.etl.service;

import org.nmcpye.datarun.jpa.etl.dao.IRepeatInstancesDao;
import org.nmcpye.datarun.jpa.etl.dao.ISubmissionValuesDao;
import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
import org.nmcpye.datarun.jpa.etl.dto.SubmissionValueRow;
import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.springframework.jdbc.core.JdbcTemplate;
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

    /**
     * Persist the normalized submission in a single transactional boundary.
     * Uses a per-submission advisory lock to serialize concurrent processing of the same submission id.
     */
    /**
     * Persist the normalized submission in a single transactional boundary.
     * Uses a per-submission advisory lock to serialize concurrent processing of the same submission id.
     */
    @Transactional
    public void persist(NormalizedSubmission ns) {
        // 0. Acquire an advisory lock for this submission (transaction-scoped)
        long lockKey = computeAdvisoryLockKey(ns.getSubmissionId());
        // Blocks until the lock is acquired. Because it's in the same transaction,
        // it will be released at commit/rollback.
        Boolean acquired = jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(?)", Boolean.class, lockKey);
        if (Boolean.FALSE.equals(acquired)) {
            // lock not acquired — decide policy: retry, return, throw, etc.
            throw new IllegalStateException("Could not acquire advisory lock for submission " + ns.getSubmissionId());
        }
        // 1. Upsert repeat instances (batched)
        batchUpsertRepeatInstances(ns.getRepeatInstances());

        // 2. Upsert submission values (DAO partitions singles vs multis)
        batchUpsertSubmissionValues(ns.getValueRows());

        // 3. Mark-and-sweep for repeat instances (existing -> toDelete),
        // then mark related values deleted
        for (Map.Entry<String, Set<String>> e
            : ns.getIncomingRepeatUids().entrySet()) {

            String repeatPath = e.getKey();
            Set<String> incomingSet = e.getValue();
            List<String> existing = repeatDao.findActiveRepeatUids(
                ns.getSubmissionId(), repeatPath);

            Set<String> toDelete = existing.stream().filter(uid ->
                !incomingSet.contains(uid)).collect(Collectors.toSet());
            if (!toDelete.isEmpty()) {
                repeatDao.markRepeatInstancesDeleted(ns.getSubmissionId(),
                    repeatPath, List.copyOf(toDelete));
                valueDao.markValuesDeletedForRepeatUids(ns.getSubmissionId(),
                    List.copyOf(toDelete));
            }
        }

        // 4. Selection-level mark-and-sweep for multi-selects
        for (Map.Entry<NormalizedSubmission.MultiSelectKey, Set<String>> ent
            : ns.getIncomingMultiSelects().entrySet()) {

            NormalizedSubmission.MultiSelectKey key = ent.getKey();
            Set<String> incomingIdentities = ent.getValue(); // may be empty => explicit clear

            // DB existing identities (COALESCE(option_id, value_text) or option_id if you require it)
            List<String> existing =
                valueDao.findSelectionIdentitiesForElementRepeat(ns.getSubmissionId(),
                    key.repeatInstanceId, key.elementId);

            if (incomingIdentities.isEmpty()) {
                // explicit clear: delete all existing selections
                if (!existing.isEmpty()) {
                    valueDao.markSelectionValuesDeletedByIdentity(ns.getSubmissionId(),
                        key.repeatInstanceId, key.elementId, existing);
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

        // Transaction commits when this method returns
        // successfully; advisory lock is released automatically.
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

    private void batchUpsertSubmissionValues(List<SubmissionValueRow> rows) {
        if (rows == null || rows.isEmpty()) return;
        int from = 0;
        while (from < rows.size()) {
            int to = Math.min(rows.size(), from + BATCH_SIZE);
            valueDao.upsertSubmissionValuesBatch(rows.subList(from, to));
            from = to;
        }
    }

    private Long computeAdvisoryLockKey(String submissionId) {
        // simple stable 64-bit mapping: use Java's built-in hashing but convert
        // to unsigned long space.
        // can be replace with a better hash (MurmurHash3) if needed.
        long h = 1469598103934665603L; // FNV offset basis
        for (char c : submissionId.toCharArray()) {
            h ^= c;
            h *= 1099511628211L; // FNV prime
        }
        return h;
    }
//    @Transactional
//    public void persist(NormalizedSubmission ns) {
//        // upsert repeat instances in batches
//        batchUpsertRepeatInstances(ns.getRepeatInstances());
//
//        // upsert submission values in batches
//        batchUpsertSubmissionValues(ns.getValueRows());
//
//        // Build incoming multi-select map:
//        // key: pair (repeatInstanceId or null, elementId) -> Set<optionId>
//        Map<String, Set<String>> incomingMulti = new HashMap<>();
//        for (SubmissionValueRow row : ns.getValueRows()) {
//            String optionId = row.getOption();
//            if (optionId == null) continue; // only care about multi-select rows
//            String repeatId = row.getRepeatInstance(); // can be null for top-level
//            String elementId = row.getElement();
//
//            String key = (repeatId == null ? "__ROOT__" : repeatId) + "|" + elementId;
//            incomingMulti.computeIfAbsent(key, k -> new HashSet<>()).add(optionId);
//        }
//
//        // For each (repeatId, elementId) that had incoming selections,
//        // compare with DB and delete missing ones.
//        for (Map.Entry<String, Set<String>> e : incomingMulti.entrySet()) {
//            String[] parts = e.getKey().split("\\|", 2);
//            String repeatIdKey = "__ROOT__".equals(parts[0]) ? null : parts[0];
//            String elementId = parts[1];
//            Set<String> incomingOptionIds = e.getValue();
//
//            // fetch existing option ids from DB
//            List<String> existingOptionIds = valueDao.findOptionIdsForElementRepeat(ns.getSubmissionId(), repeatIdKey, elementId);
//
//            // compute toDelete = existing - incoming
//            Set<String> toDelete = existingOptionIds.stream()
//                .filter(x -> !incomingOptionIds.contains(x))
//                .collect(Collectors.toSet());
//
//            if (!toDelete.isEmpty()) {
//                valueDao.markSelectionValuesDeleted(ns.getSubmissionId(), repeatIdKey,
//                    elementId, List.copyOf(toDelete));
//            }
//        }
//    }
//
//    private void batchUpsertRepeatInstances(List<RepeatInstance> reps) {
//        if (reps == null || reps.isEmpty()) return;
//        int from = 0;
//        while (from < reps.size()) {
//            int to = Math.min(reps.size(), from + BATCH_SIZE);
//            List<RepeatInstance> batch = reps.subList(from, to);
//            repeatDao.upsertRepeatInstancesBatch(batch); // implement in DAO (see SQL example)
//            from = to;
//        }
//    }
//
//    private void batchUpsertSubmissionValues(List<SubmissionValueRow> rows) {
//        if (rows == null || rows.isEmpty()) return;
//        int from = 0;
//        while (from < rows.size()) {
//            int to = Math.min(rows.size(), from + BATCH_SIZE);
//            List<SubmissionValueRow> batch = rows.subList(from, to);
//            valueDao.upsertSubmissionValuesBatch(batch);
//            from = to;
//        }
//    }
}

