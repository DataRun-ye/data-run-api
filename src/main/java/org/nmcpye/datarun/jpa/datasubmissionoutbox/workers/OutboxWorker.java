package org.nmcpye.datarun.jpa.datasubmissionoutbox.workers;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.config.datarun.OutboxProperties;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.BackoffPolicy;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.repository.OutboxEventRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Hamza Assada
 * @since 24/09/2025
 */
@Slf4j
public abstract class OutboxWorker {
    protected final OutboxEventRepository outboxRepo;
    protected final PlatformTransactionManager txManager;
    protected final OutboxProperties props;
    protected final BackoffPolicy backoffPolicy;
    protected final ExecutorService executor;
    protected final String ownerId;
    protected final TransactionTemplate txTemplate;

    protected OutboxWorker(OutboxEventRepository outboxRepo,
                           PlatformTransactionManager txManager,
                           OutboxProperties props,
                           int threadPoolSize) {
        this.outboxRepo = outboxRepo;
        this.txManager = txManager;
        this.props = props;
        this.backoffPolicy = BackoffPolicy.defaultPolicy();
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
        this.ownerId = buildOwnerId();
        this.txTemplate = new TransactionTemplate(txManager);
    }

    private String buildOwnerId() {
        String host = System.getenv().getOrDefault("HOSTNAME", "local");
        return host + "-" + UUID.randomUUID();
    }

    public abstract String eventType();

    public abstract void handle(OutboxEvent ev) throws Exception;

//    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:2000}")
    public void pollAndProcess() {
        // claim in short tx
        List<OutboxEvent> claimed = txTemplate.execute(s ->
            outboxRepo.claimBatch(props.getBatchSize(), eventType(), ownerId));
        if (claimed == null || claimed.isEmpty()) return;

        CountDownLatch latch = new CountDownLatch(claimed.size());
        for (OutboxEvent ev : claimed) {
            executor.submit(() -> {
                try {
                    processSingle(ev);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            // wait bounded time per batch
            boolean finished = latch.await(props.getBatchTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                log.warn("Batch processing timed out for eventType={} claimed={}", eventType(), claimed.size());
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void processSingle(OutboxEvent ev) {
        long id = ev.getId();
        try {
            handle(ev);
            outboxRepo.markDone(id, Instant.now());
        } catch (Exception ex) {
            // attempts was already incremented on claim; treat ev.getAttempts() as current attempt
            int attempts = ev.getAttempts() == null ? 1 : ev.getAttempts();
            if (attempts >= props.getMaxAttempts()) {
                outboxRepo.markDlq(id, truncate(ex));
                log.warn("Event id={} exceeded max attempts {}, marked FAILED", id, attempts);
            } else {
                Instant next = backoffPolicy.next(attempts); // use attempts (not attempts+1) because we incremented at claim
                outboxRepo.reschedule(id, truncate(ex), next);
                log.info("Event id={} rescheduled (attempt {}) -> next at {}", id, attempts, next);
            }
        }
    }

    private String truncate(Throwable t) {
        String m = t.getMessage();
        if (m == null) return t.getClass().getSimpleName();
        return m.length() <= 2000 ? m : m.substring(0, 2000);
    }

    // call during shutdown if needed
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
