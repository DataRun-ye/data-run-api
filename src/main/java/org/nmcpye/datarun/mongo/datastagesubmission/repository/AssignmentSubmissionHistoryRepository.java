//package org.nmcpye.datarun.mongo.repository;
//
//import org.javers.spring.annotation.JaversSpringDataAuditable;
//import org.nmcpye.datarun.mongo.common.repository.MongoAuditableRepository;
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
//    @Query("{'id': ?0, 'entries.submissionTeam': ?1}")
//    List<AssignmentSubmissionHistory> findByUidAndTeam(String id, String team);
//
//    @Query(value = "{'id': ?0}", sort = "{'entries.entryDate': -1}")
//    Optional<AssignmentSubmissionHistory> findLastEntryByUid(String id);
//
//    @Query(value = "{'id': ?0, 'entries.assignedTeam': ?1}", sort = "{'entries.entryDate': -1}")
//    Optional<AssignmentSubmissionHistory> findLastEntryByUidAndAssignedTeam(String id, String assignedTeam);
//
//    //submissionUser
//    @Query(value = "{'id': ?0, 'entries.submissionTeam': ?1}", sort = "{'entries.entryDate': -1}")
//    Optional<AssignmentSubmissionHistory> findLastEntryByUidAndTeam(String id, String team);
//
//    @Query(value = "{'id': ?0, 'entries.submissionUser': ?1}", sort = "{'entries.entryDate': -1}")
//    Optional<AssignmentSubmissionHistory> findLastEntryByUidAndUser(String id, String login);
//}
