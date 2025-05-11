package org.nmcpye.datarun.assignmentsubmissionhistory;

import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.mongo.domain.AssignmentSubmissionHistory;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionHistoryService
    extends AuditableObjectService<AssignmentSubmissionHistory, String> {
    void updateSubmissionHistory(DataFormSubmission submission);

    List<AssignmentSubmissionHistory> findByUidAndTeamId(String uid, String teamId);

    Optional<AssignmentSubmissionHistory.HistoryEntry> findLastEntryByUidAndTeamId(String uid, String teamId);

    Optional<AssignmentSubmissionHistory.HistoryEntry> findLastEntryByUid(String uid);

    Optional<AssignmentSubmissionHistory.HistoryEntry> findLastEntryByUidAndUser(String uid, String user);
}
