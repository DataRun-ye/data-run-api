package org.nmcpye.datarun.jpa.datasubmissionoutbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.config.datarun.OutboxProperties;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.repository.OutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Hamza Assada
 * @since 24/09/2025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReclaimStaleJob {

    private final OutboxEventRepository outboxRepo;
    private final OutboxProperties props;

    @Scheduled(fixedDelayString = "${outbox.relay.reclaim-interval-ms:60000}")
    public void reclaim() {
        // Implement a repository method that runs reclaim SQL and returns list of reclaimed ids
        List<Long> reclaimed = outboxRepo.reclaimStale(processingTimeoutSqlInterval(props.getVisibilityTimeoutSeconds()));
        if (!reclaimed.isEmpty()) {
            log.warn("Reclaimed {} stale outbox rows: {}", reclaimed.size(), reclaimed);
        }
    }

    private int processingTimeoutSqlInterval(int seconds) {
        return seconds;
    } // pass-through helper
}
