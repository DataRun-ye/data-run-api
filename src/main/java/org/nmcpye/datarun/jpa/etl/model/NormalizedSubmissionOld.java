//package org.nmcpye.datarun.jpa.etl.model;
//
//import lombok.Builder;
//import lombok.Getter;
//import lombok.experimental.Accessors;
//import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
//import org.nmcpye.datarun.jpa.etl.dto.SubmissionValueRow;
//import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
//import org.nmcpye.datarun.security.CurrentUserDetails;
//
//import java.time.Instant;
//import java.util.*;
//
///**
// * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
// */
//@Getter
//@Accessors(chain = true)
//public class NormalizedSubmissionOld {
//    private final String submissionId;
//    private final String templateId;
//    private final String assignmentId;
//    private final String createdBy;
//    private final String lastModifiedBy;
//    private final Instant createdDate;
//    private final Instant clientUpdatedAt;
//    private final Instant lastModifiedDate;
//    /**
//     * All normalized value rows
//     */
//    private final List<SubmissionValueRow> valueRows = new ArrayList<>();
//
//    /**
//     * All repeat instances that must exist or be updated
//     */
//    private final List<RepeatInstance> repeatInstances = new ArrayList<>();
//
//    /**
//     * per repeatPath incoming UIDs (for mark-and-sweep)
//     */
//    private final Map<String, Set<String>> incomingRepeatUids = new HashMap<>();
//
//    @Builder(toBuilder = true)
//    public NormalizedSubmissionOld(String submissionId, String templateId, String assignmentId,
//                                   String createdBy, String lastModifiedBy,
//                                   Instant createdDate, Instant clientUpdatedAt,
//                                   Instant lastModifiedDate) {
//        this.submissionId = submissionId;
//        this.templateId = templateId;
//        this.assignmentId = assignmentId;
//        this.createdBy = createdBy;
//        this.lastModifiedBy = lastModifiedBy;
//        this.createdDate = createdDate;
//        this.clientUpdatedAt = clientUpdatedAt;
//        this.lastModifiedDate = lastModifiedDate;
//    }
//
//    // ----------------- getters and add helpers ----------------
//
//    /**
//     * Add a normalized value for upsert or update
//     *
//     * @param r a normalized value row
//     */
//    public void addValueRow(SubmissionValueRow r) {
//        valueRows.add(r);
//    }
//
//    /**
//     * @param ri a repeat instances that must exist or be updated
//     */
//    public void addRepeatInstance(RepeatInstance ri) {
//        repeatInstances.add(ri);
//    }
//
//
//    /**
//     * adds per repeatPath incoming UIDs (for mark-and-sweep)
//     *
//     * @param repeatPath path
//     * @param uid        incoming uid
//     */
//    public void addIncomingUid(String repeatPath, String uid) {
//        incomingRepeatUids.computeIfAbsent(repeatPath, k -> new HashSet<>()).add(uid);
//    }
//
//    public static NormalizedSubmissionOld initialValueBuilder(DataFormSubmission submission,
//                                                              CurrentUserDetails currentUser) {
//        final var currentUserLogin = currentUser != null ? currentUser.getUsername() : null;
//        final NormalizedSubmissionOld.NormalizedSubmissionBuilder nsBuilder = NormalizedSubmissionOld.builder()
//            .submissionId(submission.instanceId())
//            .assignmentId(submission.getAssignment())
//            .templateId(submission.getForm())
//            .createdBy(submission.getCreatedBy())
//            .lastModifiedBy(submission.getLastModifiedBy())
//            .createdDate(submission.getCreatedDate())
//            .lastModifiedDate(submission.getLastModifiedDate())
//            .clientUpdatedAt(submission.getFinishedEntryTime());
//
//        if (nsBuilder.createdBy == null) {
//            nsBuilder.createdBy(currentUserLogin);
//        }
//        if (nsBuilder.lastModifiedBy == null) {
//            nsBuilder.lastModifiedBy(currentUserLogin);
//        }
//        if (nsBuilder.createdDate == null) {
//            nsBuilder.createdDate(Instant.now());
//        }
//        if (nsBuilder.lastModifiedDate == null) {
//            nsBuilder.lastModifiedDate(Instant.now());
//        }
//
//        if (nsBuilder.clientUpdatedAt == null) {
//            nsBuilder.clientUpdatedAt(Instant.now());
//        }
//
//        return nsBuilder.build();
//    }
//}
