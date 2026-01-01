//package org.nmcpye.datarun.jpa.etl.model;
//
//import lombok.Getter;
//import lombok.experimental.Accessors;
//import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
//import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
///**
// * Container holding the complete, normalized representation of a single submission.
// *
// * <p>Produced by {@code Normalizer} and consumed by persisters/DAOs.
// * Contains:
// * <ul>
// *   <li>submission identifiers and context</li>
// *   <li>a list of RepeatInstance DTOs (repeat sections)</li>
// *   <li>a list of ElementDataValue rows (normalized element values)</li>
// * </ul>
// *
// * <p>Design notes:
// * <ul>
// *   <li>Mutable lists for incremental population during normalization.</li>
// *   <li>Lightweight: does not itself persist anything; intended as an in-memory transfer object.</li>
// * </ul>
// *
// * @author Hamza Assada
// * @since 13/08/2025
// */
//@Getter
//@Accessors(chain = true)
//public class NormalizedOutboxSubmission extends NormalizedSubmission {
//    private final UUID ingestId;
//    private final Long outboxId;
//
//    private final List<RepeatInstance> repeatInstances = new ArrayList<>();
//    private final List<ElementDataValue> values = new ArrayList<>();
//
//    public NormalizedOutboxSubmission(String submissionUid, String templateUid, String assignmentUid,
//                                      UUID ingestId, Long outboxId) {
//        super(submissionUid, templateUid, assignmentUid);
//        this.ingestId = ingestId;
//        this.outboxId = outboxId;
//    }
//}
