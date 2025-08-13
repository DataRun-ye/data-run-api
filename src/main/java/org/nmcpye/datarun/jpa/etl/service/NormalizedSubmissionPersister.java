package org.nmcpye.datarun.jpa.etl.service;

import org.nmcpye.datarun.jpa.etl.dao.IRepeatInstancesDao;
import org.nmcpye.datarun.jpa.etl.dao.ISubmissionValuesDao;
import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
import org.nmcpye.datarun.jpa.etl.dto.SubmissionValueRow;
import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    public NormalizedSubmissionPersister(IRepeatInstancesDao repeatDao, ISubmissionValuesDao valueDao) {
        this.repeatDao = repeatDao;
        this.valueDao = valueDao;
    }

    @Transactional
    public void persist(NormalizedSubmission ns) {
        // 1) upsert repeat instances in batches
        batchUpsertRepeatInstances(ns.getRepeatInstances());

        // 2) upsert submission values in batches
        batchUpsertSubmissionValues(ns.getValueRows());

        // 3) mark-and-sweep per repeatPath
        for (Map.Entry<String, Set<String>> e : ns.getIncomingRepeatUids().entrySet()) {
            String repeatPath = e.getKey();
            List<String> incoming = new ArrayList<>(e.getValue());
            List<String> existing = repeatDao.findActiveRepeatUids(ns.getSubmissionId(), repeatPath);

            Set<String> toDelete = existing.stream().filter(uid -> !e.getValue().contains(uid)).collect(Collectors.toSet());
            if (!toDelete.isEmpty()) {
                repeatDao.markRepeatInstancesDeleted(ns.getSubmissionId(), repeatPath, new ArrayList<>(toDelete));
                valueDao.markValuesDeletedForRepeatUids(ns.getSubmissionId(), new ArrayList<>(toDelete));
            }
        }
    }

    private void batchUpsertRepeatInstances(List<RepeatInstance> reps) {
        if (reps == null || reps.isEmpty()) return;
        int from = 0;
        while (from < reps.size()) {
            int to = Math.min(reps.size(), from + BATCH_SIZE);
            List<RepeatInstance> batch = reps.subList(from, to);
            repeatDao.upsertRepeatInstancesBatch(batch); // implement in DAO (see SQL example)
            from = to;
        }
    }

    private void batchUpsertSubmissionValues(List<SubmissionValueRow> rows) {
        if (rows == null || rows.isEmpty()) return;
        int from = 0;
        while (from < rows.size()) {
            int to = Math.min(rows.size(), from + BATCH_SIZE);
            List<SubmissionValueRow> batch = rows.subList(from, to);
            valueDao.upsertSubmissionValuesBatch(batch);
            from = to;
        }
    }
}

