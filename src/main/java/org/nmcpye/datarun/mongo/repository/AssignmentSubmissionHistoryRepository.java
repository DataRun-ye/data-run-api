//package org.nmcpye.datarun.mongo.repository;
//
//import org.javers.spring.annotation.JaversSpringDataAuditable;
//import org.nmcpye.datarun.common.mongo.repository.MongoAuditableRepository;
//import org.nmcpye.datarun.mongo.domain.AssignmentSubmissionHistory;
//import org.springframework.data.mongodb.repository.Query;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//@JaversSpringDataAuditable
//public interface AssignmentSubmissionHistoryRepository extends MongoAuditableRepository<AssignmentSubmissionHistory>/*, AssignmentSubmissionHistoryRepositoryCustom*/ {
//
//    @Query("{'uid': ?0, 'entries.submissionTeam': ?1}")
//    List<AssignmentSubmissionHistory> findByUidAndTeam(String uid, String team);
//
//    @Query(value = "{'uid': ?0}", sort = "{'entries.entryDate': -1}")
//    Optional<AssignmentSubmissionHistory> findLastEntryByUid(String uid);
//
//    @Query(value = "{'uid': ?0, 'entries.assignedTeam': ?1}", sort = "{'entries.entryDate': -1}")
//    Optional<AssignmentSubmissionHistory> findLastEntryByUidAndAssignedTeam(String uid, String assignedTeam);
//
//    //submissionUser
//    @Query(value = "{'uid': ?0, 'entries.submissionTeam': ?1}", sort = "{'entries.entryDate': -1}")
//    Optional<AssignmentSubmissionHistory> findLastEntryByUidAndTeam(String uid, String team);
//
//    @Query(value = "{'uid': ?0, 'entries.submissionUser': ?1}", sort = "{'entries.entryDate': -1}")
//    Optional<AssignmentSubmissionHistory> findLastEntryByUidAndUser(String uid, String login);
//}
