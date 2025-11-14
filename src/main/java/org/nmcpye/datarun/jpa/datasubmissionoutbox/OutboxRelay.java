package org.nmcpye.datarun.jpa.datasubmissionoutbox;

import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.config.datarun.OutboxProperties;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.repository.OutboxEventRepository;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.workers.OutboxWorker;
import org.nmcpye.datarun.jpa.etl.service.EtlCoordinatorService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OutboxRelay: claims batches of outbox events and processes them concurrently.
 * <p>
 * - Uses {@link OutboxEventRepository#claimBatch(int) claimBatch(int)} to safely claim rows with SKIP LOCKED.
 * <p>
 * - Submits each claimed event to a bounded thread pool.
 * <p>
 * - On success: {@link OutboxEventRepository#markDone(long, Instant) markDone(long, Instant)};
 * <p>
 * - on failure:{@link OutboxEventRepository#reschedule(long, String, Instant) reschedule(long, String, Instant)}
 * or {@link OutboxEventRepository#markDlq(long, String) markFailed(long, String)}
 * when {@link OutboxProperties#getMaxAttempts()} exceeded.
 *
 * @author Hamza Assada
 * @since 15/08/2025 (7amza.it@gmail.com)
 */
@Component
@Slf4j
public class OutboxRelay implements DisposableBean {
    private final OutboxEventRepository outboxRepo;
    private final EtlCoordinatorService etlService;
    private final PlatformTransactionManager txManager;
    private final BackoffPolicy backoffPolicy;
    private final OutboxProperties props;
    private final ExecutorService workerPool;

    // metrics
    private final Counter processedSuccess;
    private final Counter processedRescheduled;
    private final Counter processedFailed;
    private final Timer processingTimer;
    private final ConcurrentMap<String, OutboxWorker> workersMap;

    public OutboxRelay(OutboxEventRepository outboxRepo,
                       EtlCoordinatorService etlService,
                       PlatformTransactionManager txManager,
                       OutboxProperties props,
                       MeterRegistry meterRegistry, List<OutboxWorker> workers) {

        this.outboxRepo = outboxRepo;
        this.etlService = etlService;
        this.txManager = txManager;
        this.props = props;
        this.backoffPolicy = BackoffPolicy.defaultPolicy();
        this.workerPool = Executors.newFixedThreadPool(Math.max(1, props.getWorkerThreads()),
            runnable -> {
                Thread t = new Thread(runnable);
                t.setName("outbox-relay-worker-" + t.getId());
                t.setDaemon(true);
                return t;
            });

        // metrics
        this.processedSuccess = meterRegistry.counter("outbox.relay.processed.success");
        this.processedRescheduled = meterRegistry.counter("outbox.relay.processed.rescheduled");
        this.processedFailed = meterRegistry.counter("outbox.relay.processed.failed");
        this.processingTimer = meterRegistry.timer("outbox.relay.processing.time");
        this.workersMap = workers.stream()
            .collect(Collectors.toConcurrentMap(OutboxWorker::eventType, Function.identity()));
    }

    /**
     * Scheduled poller. We use fixedDelay so that interval begins after processing completes.
     * Poll interval is configurable via outbox.relay.poll-interval-ms.
     */
//    @Scheduled(fixedDelayString = "${outbox.relay.poll-interval-ms:2000}")
    public void pollAndProcess() {
        try {
            // claim in a short transaction to leverage FOR UPDATE SKIP LOCKED
            TransactionTemplate tt = new TransactionTemplate(txManager);
            List<OutboxEvent> claimed = tt.execute(status ->
                outboxRepo.claimBatch(props.getBatchSize()));

            if (claimed == null || claimed.isEmpty()) {
                return;
            }

            log.debug("Claimed {} outbox events", claimed.size());

            // process each event concurrently but bounded by the workerPool
            CountDownLatch latch = new CountDownLatch(claimed.size());

            for (OutboxEvent ev : claimed) {
                workerPool.submit(() -> {
                    try {
                        processEvent(ev);
                    } catch (Throwable t) {
                        log.error("Unexpected error processing outbox event id={}: {}", ev.getId(), t.getMessage(), t);
                        // best-effort mark failed/reschedule
                        safeHandleProcessingFailure(ev, t);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for this batch to finish before continuing (prevents unbounded backlog per poll)
            try {
                boolean finished = latch.await(10, TimeUnit.MINUTES); // tune timeout as appropriate
                if (!finished) {
                    log.warn("Outbox batch processing timed out for claimed size={}", claimed.size());
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

        } catch (Exception ex) {
            log.error("Outbox relay poll failed", ex);
        }
    }

    private void processEvent(OutboxEvent ev) {
        long id = ev.getId();
        JsonNode payload = ev.getPayload();
        String submissionId = payload != null && payload.has("submissionId") ? payload.get("submissionId").asText() : null;

        if (submissionId == null || submissionId.isBlank()) {
            log.error("OutboxEvent {} missing submissionId — marking FAILED", id);
            outboxRepo.markDlq(id, "missing submissionId in payload");
            processedFailed.increment();
            return;
        }

        // run ETL and measure time
        try {
            workersMap.get(ev.getEventType()).handle(ev);
            if (ev.getEventType().equals("submission.saved")) {
                processingTimer.record(() -> {
                    etlService.processSubmission(submissionId, true);
//                    extractor.processSubmission(e.getPayload().get("submissionId").asText());
                });
                // extractor should upsert extraction_manifest and in same TX create manifest.created outbox row
            } else if (ev.getEventType().equals("manifest.created")) {
                processingTimer.record(() -> {
                    etlService.processSubmission(submissionId, true);
//                    etlService.processManifest(ev.getPayload().get("manifestId").asText());
                });
            }

            // mark done
            outboxRepo.markDone(id, Instant.now());
            processedSuccess.increment();
            log.debug("OutboxEvent {} processed successfully for submission {}", id, submissionId);

        } catch (Exception ex) {
            log.error("OutboxEvent {} processing failed for submission {}: {}", id, submissionId, ex.getMessage());

            int attempts = ev.getAttempts() == null ? 1 : ev.getAttempts(); // claimBatch incremented attempts already
            // If attempts already exceeded threshold, mark FAILED
            if (attempts >= props.getMaxAttempts()) {
                outboxRepo.markDlq(id, truncate(ex));
                processedFailed.increment();
                log.warn("OutboxEvent {} exceeded max attempts {} -> marked FAILED", id, attempts);
                return;
            }

            // Exponential backoff and reschedule
            Instant next = backoffPolicy.next(attempts + 1);
            outboxRepo.reschedule(id, truncate(ex), next);
            processedRescheduled.increment();
            log.info("OutboxEvent {} rescheduled (attempt {}) -> next at {}", id, attempts + 1, next);
        }
    }

    // best-effort failure path if unexpected throwable in the worker thread before normal handling
    private void safeHandleProcessingFailure(OutboxEvent ev, Throwable t) {
        try {
            int attempts = ev.getAttempts() == null ? 1 : ev.getAttempts();
            if (attempts >= props.getMaxAttempts()) {
                outboxRepo.markDlq(ev.getId(), truncate(t));
                processedFailed.increment();
            } else {
                Instant next = backoffPolicy.next(attempts + 1);
                outboxRepo.reschedule(ev.getId(), truncate(t), next);
                processedRescheduled.increment();
            }
        } catch (Exception ex) {
            log.error("Failed to mark/reschedule outbox event {} after processing error: {}", ev.getId(), ex.getMessage(), ex);
        }
    }

    private String truncate(Throwable t) {
        String msg = t.getMessage();
        return msg == null ? t.getClass().getSimpleName() : (msg.length() <= 2000 ? msg : msg.substring(0, 2000));
    }

    // ensure executor service shutdown on destroy (optional, Spring will shut down threads on exit)
    @Override
    public void destroy() throws Exception {
        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(10, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
