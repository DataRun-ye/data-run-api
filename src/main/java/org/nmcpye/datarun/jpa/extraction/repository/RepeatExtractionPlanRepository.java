//package org.nmcpye.datarun.jpa.extraction.repository;
//
//import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
//import org.nmcpye.datarun.jpa.extraction.RepeatExtractionPlan;
//import org.springframework.stereotype.Repository;
//
//import java.util.Optional;
//import java.util.UUID;
//
///**
// * @author Hamza Assada
// * @since 21/09/2025
// */
//@Repository
//public interface RepeatExtractionPlanRepository
//    extends BaseJpaIdentifiableRepository<RepeatExtractionPlan, UUID> {
//    Optional<RepeatExtractionPlan> findTopByRepeatUidOrderByCreatedAtDesc(String repeatUid);
//}
