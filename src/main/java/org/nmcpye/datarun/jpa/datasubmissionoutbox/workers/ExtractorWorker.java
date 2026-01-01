//package org.nmcpye.datarun.jpa.datasubmissionoutbox.workers;
//
//import lombok.extern.slf4j.Slf4j;
//import org.nmcpye.datarun.config.datarun.OutboxProperties;
//import org.nmcpye.datarun.jpa.datasubmission.service.ExtractorService;
//import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;
//import org.nmcpye.datarun.jpa.datasubmissionoutbox.repository.OutboxEventRepository;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.support.TransactionTemplate;
//
///**
// * @author Hamza Assada
// * @since 24/09/2025
// */
//@Slf4j
//public class ExtractorWorker extends OutboxWorker {
//
//    private final ExtractorService extractorService;
//    private final TransactionTemplate tx;
//
//    public ExtractorWorker(OutboxEventRepository outboxRepo,
//                           PlatformTransactionManager txManager,
//                           OutboxProperties props,
//                           ExtractorService extractorService,
//                           int threadPoolSize) {
//        super(outboxRepo, txManager, props, threadPoolSize);
//        this.extractorService = extractorService;
//        this.tx = new TransactionTemplate(txManager);
//    }
//
//    @Override
//    public String eventType() {
//        return "submission.saved";
//    }
//
//    @Override
//    public void handle(OutboxEvent ev) throws Exception {
//        // payload expected: {"submissionId":"..."}
//        String submissionId = ev.getPayload() == null || ev.getPayload().get("submissionId") == null
//            ? null
//            : ev.getPayload().get("submissionId").asText();
//
//        if (submissionId == null || submissionId.isBlank()) {
//            throw new IllegalArgumentException("missing submissionId");
//        }
//
//        // The extractor does heavy canonicalization, but we want the manifest upsert + manifest.outbox insert to be atomic.
//        // Let extractorService.runExtractorTx do that in a transaction.
//        tx.execute(status -> {
//            extractorService.extractAndPublishManifest(submissionId);
//            return null;
//        });
//        // If no exception, success -> caller will markDone.
//    }
//}
