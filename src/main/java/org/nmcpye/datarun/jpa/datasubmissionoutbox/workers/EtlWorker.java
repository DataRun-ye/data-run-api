package org.nmcpye.datarun.jpa.datasubmissionoutbox.workers;

import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.config.datarun.OutboxProperties;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.repository.OutboxEventRepository;
import org.nmcpye.datarun.jpa.etl.service.EtlCoordinatorService;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Hamza Assada
 * @since 24/09/2025
 */
@Slf4j
public class EtlWorker extends OutboxWorker {

    private final EtlCoordinatorService etlService;

    public EtlWorker(OutboxEventRepository outboxRepo,
                     PlatformTransactionManager txManager,
                     OutboxProperties props,
                     EtlCoordinatorService etlService,
                     int threadPoolSize) {
        super(outboxRepo, txManager, props, threadPoolSize);
        this.etlService = etlService;
    }

    @Override
    public String eventType() {
        return "manifest.created";
    }

    @Override
    public void handle(OutboxEvent ev) throws Exception {
        String manifestId = ev.getPayload() == null || ev.getPayload().get("manifestId") == null
            ? null
            : ev.getPayload().get("manifestId").asText();

        if (manifestId == null || manifestId.isBlank()) {
            throw new IllegalArgumentException("missing manifestId");
        }

        // ETL work should be idempotent; do it outside claim TX
//        etlService.processManifest(manifestId);
    }
}
