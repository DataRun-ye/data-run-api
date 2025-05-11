package org.nmcpye.datarun.assignmentsubmissionhistory.repository;

import org.nmcpye.datarun.mongo.domain.AssignmentSubmissionHistory;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionHistoryRepositoryCustom {
    List<AssignmentSubmissionHistory> findByUidAndTeam(String uid, String team);

    Optional<AssignmentSubmissionHistory> findLastEntryByUid(String uid);

    Optional<AssignmentSubmissionHistory> findLastEntryByUidAndAssignedTeam(String uid, String assignedTeam);

    Optional<AssignmentSubmissionHistory> findLastEntryByUidAndTeam(String uid, String team);

    Optional<AssignmentSubmissionHistory> findLastEntryByUidAndUser(String uid, String login);
}
