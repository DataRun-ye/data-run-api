package org.nmcpye.datarun.mongo.service;

import org.nmcpye.datarun.mongo.domain.AssignmentSubmissionHistory;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionHistoryService extends IdentifiableMongoService<AssignmentSubmissionHistory> {
    void updateSubmissionHistory(DataFormSubmission submission);

    List<AssignmentSubmissionHistory> findByUidAndTeamId(String uid, String teamId);

    Optional<AssignmentSubmissionHistory.HistoryEntry> findLastEntryByUidAndTeamId(String uid, String teamId);

    Optional<AssignmentSubmissionHistory.HistoryEntry> findLastEntryByUid(String uid);

    Optional<AssignmentSubmissionHistory.HistoryEntry> findLastEntryByUidAndUser(String uid, String user);
}
