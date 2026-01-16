package org.nmcpye.datarun.ledger;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionScheduler {

    private final ClaimService claimService;
    private final ProcessingWorker processingWorker;

    @Value("${etl.claim.batch-size:20}")
    private int batchSize;

    @Value("${etl.claim.stale-interval-minutes:30}")
    private String claimStaleInterval; // pass like "30 minutes"

    @Value("${etl.claim.concurrent: false}")
    private boolean concurrent;

    @Value("${etl.claim.thread-pool-size:4}")
    private int threadPoolSize;

    private String workerId;
    private ExecutorService executor;

    @PostConstruct
    public void init() {
        // generate a reasonably-unique worker id: host:pid:uuid
        try {
            String host = InetAddress.getLocalHost().getHostName();
            workerId = host + ":" + UUID.randomUUID().toString().substring(0,8);
        } catch (Exception e) {
            workerId = "worker-" + UUID.randomUUID().toString().substring(0,8);
        }
        if (concurrent) {
            executor = Executors.newFixedThreadPool(threadPoolSize);
        }
        log.info("SubmissionScheduler {} started (concurrent={})", workerId, concurrent);
    }

    /**
     * Scheduled to run periodically. Adjust cron/fixedDelay as you need.
     * This example runs every 15 seconds.
     */
//    @Scheduled(fixedDelayString = "${etl.claim.fixed-delay-ms:15000}")
    public void tick() {
        try {
            // claim a batch
            List<String> claimed = claimService.claimBatch(workerId, batchSize, claimStaleInterval);
            if (claimed == null || claimed.isEmpty()) return;

            log.info("Worker {} processing {} claimed submissions", workerId, claimed.size());
            for (String submissionUid : claimed) {
                if (concurrent) {
                    executor.submit(() -> processingWorker.process(submissionUid, workerId));
                } else {
                    processingWorker.process(submissionUid, workerId);
                }
            }
        } catch (Exception ex) {
            log.error("Claiming loop failed: {}", ex.getMessage(), ex);
        }
    }
}
