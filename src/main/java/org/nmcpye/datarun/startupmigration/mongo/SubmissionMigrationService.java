//package org.nmcpye.datarun.mongo.service.submissionmigration;
//
//import com.jayway.jsonpath.JsonPath;
//import org.apache.kafka.common.utils.CloseableIterator;
//import org.nmcpye.datarun.drun.postgres.domain.enumeration.AssignmentStatus;
//import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
//import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
//import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
//import org.nmcpye.datarun.mongo.domain.DataFormSubmissionBu;
//import org.nmcpye.datarun.mongo.repository.DataFormSubmissionBuRepository;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.stereotype.Service;
//
//import java.util.Optional;
//import java.util.concurrent.atomic.AtomicInteger;
//
//@Service
//public class SubmissionMigrationService {
//
//    private final DataFormSubmissionBuRepository repository;
//    private final ActivityRelationalRepositoryCustom activityRepository;
//    private final TeamRelationalRepositoryCustom teamRepository;
//    private final AssignmentRelationalRepositoryCustom assignmentRepository;
//    private final MongoTemplate mongoTemplate;
//
//    public SubmissionMigrationService(DataFormSubmissionBuRepository repository,
//                                      ActivityRelationalRepositoryCustom activityRepository,
//                                      TeamRelationalRepositoryCustom teamRepository, AssignmentRelationalRepositoryCustom assignmentRepository, MongoTemplate mongoTemplate) {
//        this.activityRepository = activityRepository;
//        this.teamRepository = teamRepository;
//        this.repository = repository;
//        this.assignmentRepository = assignmentRepository;
//        this.mongoTemplate = mongoTemplate;
//    }
//
//
//    public void migrateLargeDataset() {
//        Query query = Query.query(Criteria.where("form").is("Tcf3Ks9ZRpB").and("activity").is("lNfYSY0wNFA"));
//        AtomicInteger batchCount = new AtomicInteger(0);
//        try (CloseableIterator<DataFormSubmissionBu> stream =
//                 (CloseableIterator<DataFormSubmissionBu>) mongoTemplate.stream(query, DataFormSubmissionBu.class).iterator()) {
//
//            stream.forEachRemaining(v1Submission -> {
//                DataFormSubmissionBu v2Submission = processAssignmentAndTeamSubmission(v1Submission);
//
//                mongoTemplate.save(v2Submission);
//
////                // Optional: Batch writes for better performance
////                if (batchCount.incrementAndGet() % 1000 == 0) {
////                    mongoTemplate.save(flush: true);
////                }
//            });
//        }
//    }
//
//    private DataFormSubmissionBu processAssignmentAndTeamSubmission(DataFormSubmissionBu v1Submission) {
//        if (v1Submission != null) {
//            Optional<String> orgUnit = Optional.ofNullable(JsonPath.read(v1Submission.getFormData(), "$.mainSection.locationStatus.orgUnit"));
//            Optional<String> Status = Optional.ofNullable(JsonPath.read(v1Submission.getFormData(), "$.mainSection.locationStatus.status"));
//            Optional<String> username = Optional.ofNullable(JsonPath.read(v1Submission.getFormData(), "$._username"));
//            Optional<String> activity = Optional.ofNullable(JsonPath.read(v1Submission.getFormData(), "$._activity"));
//
//            orgUnit.ifPresent(v1Submission::setActivity);
//            Status.map(AssignmentStatus::valueOf).ifPresent(v1Submission::setStatus);
//            activity.ifPresent(v1Submission::setActivity);
//
//            username.flatMap(s -> teamRepository.findFirstByActivityAndUser(v1Submission.getActivity(), s))
//                .ifPresent((t) -> {
//                    v1Submission.setTeam(t.getUid());
//                    v1Submission.getFormData().put("_team", t.getUid());
//
//                    orgUnit.flatMap(string -> assignmentRepository
//                            .findFirstByTeam_UidAndOrgUnit_Uid(t.getUid(), string))
//                        .ifPresent((assignment) -> {
//                            v1Submission.setAssignment(assignment.getUid());
//                            v1Submission.getFormData().putIfAbsent("_assignment", assignment.getUid());
//                        });
//                });
//        }
//        return v1Submission;
//    }
//}
