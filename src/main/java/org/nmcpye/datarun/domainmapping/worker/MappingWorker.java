//package org.nmcpye.datarun.domainmapping.worker;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.nmcpye.datarun.domainmapping.dto.DomainDto;
//import org.nmcpye.datarun.domainmapping.model.DataElementDomainMapping;
//import org.nmcpye.datarun.domainmapping.model.EtlRun;
//import org.nmcpye.datarun.domainmapping.repo.*;
//import org.nmcpye.datarun.domainmapping.service.MappingExecutor;
//import org.nmcpye.datarun.utils.IdempotencyKeyGenerator;
//import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.UUID;
//
///**
// * @author Hamza Assada
// * @since 23/09/2025
// */
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class MappingWorker {
//
//    private final EtlRunRepository etlRunRepo; // custom repo to find unprocessed runs
//    private final ElementDataValueRepository edvRepo; // fetch edv rows for an etl_run
//    private final DataElementDomainMappingRepository mappingRepo;
//    private final MappingRunRepository mappingRunRepo;
//    private final MappingExecutionLogRepository executionLogRepo;
//    private final MappingDeadLetterRepository deadLetterRepo;
//    private final MappingExecutor mappingExecutor;
//    private final DomainValueRepository domainValueRepo;
//
//    @Scheduled(fixedDelayString = "${mapping.worker.poll-ms:5000}")
//    public void poll() {
//        // simple single-threaded poll; you can implement parallel workers later.
//        etlRunRepo.findNextUnprocessed().ifPresent(this::processEtlRun);
//    }
//
//    @Transactional
//    public void processEtlRun(EtlRun etlRun) {
//        UUID mappingRunId = mappingRunRepo.createRun(etlRun.getId()); // returns run uid
//        try {
//            List<ElementDataValue> edvs = edvRepo.findByEtlRunId(etlRun.getId());
//            // prefetch mappings by template version
//            List<DataElementDomainMapping> mappings = mappingRepo.findPublishedByTemplateVersion(etlRun.getTemplateVersionUid());
//            var mappingByEtc = mappings.stream().collect(java.util.stream.Collectors.toMap(DataElementDomainMapping::getEtcUid, m -> m));
//            for (ElementDataValue edv : edvs) {
//                DataElementDomainMapping mapping = mappingByEtc.get(edv.getEtcUid());
//                if (mapping == null) {
//                    executionLogRepo.log(mappingRunId, edv.getId(), null, "UNMAPPED", null);
//                    continue;
//                }
//                try {
//                    // compute structural edv key (contract)
//                    String edvStructuralKey = IdempotencyKeyGenerator.edvStructuralKey(
//                        edv.getSubmissionUid(), edv.getRepeatInstanceId(), edv.getCanonicalPath(), edv.getCanonicalElementUid());
//                    List<DomainDto> outputs = mappingExecutor.applyMapping(mapping, edv, edvStructuralKey, etlRun.getId());
//                    for (DomainDto dto : outputs) {
//                        domainValueRepo.upsertDomainValue(dto);
//                        executionLogRepo.log(mappingRunId, edv.getId(), mapping.getMappingUid(), "OK", dto.toString());
//                    }
//                } catch (Exception ex) {
//                    log.error("Mapping failure for edv {} mapping {}", edv.getId(), mapping.getMappingUid(), ex);
//                    deadLetterRepo.save(edv.getId(), mapping.getMappingUid(), ex);
//                    executionLogRepo.log(mappingRunId, edv.getId(), mapping.getMappingUid(), "DLQ", ex.getMessage());
//                }
//            }
//            mappingRunRepo.markSuccess(mappingRunId);
//            etlRunRepo.markMappingProcessed(etlRun.getId(), mappingRunId);
//        } catch (Exception ex) {
//            log.error("Mapping run failed for etlRun {}", etlRun.getId(), ex);
//            mappingRunRepo.markFailed(mappingRunId, ex.getMessage());
//        }
//    }
//}
