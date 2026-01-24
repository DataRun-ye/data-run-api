package org.nmcpye.datarun.jpa.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class LastSeenServiceOld {
    private final JdbcTemplate jdbc;
    private final ConcurrentHashMap<String, Long> lastWrite = new ConcurrentHashMap<>();
    private final long throttleMs = Duration.ofMinutes(5).toMillis();
    private final Executor executor;
    private final TransactionTemplate txTemplate;

    public LastSeenServiceOld(JdbcTemplate jdbc,
                              @Qualifier("lastSeenExecutor") Executor executor, TransactionTemplate txTemplate) {
        this.jdbc = jdbc;
        this.executor = executor;
        this.txTemplate = txTemplate;
    }

    public void touch(String userId, Instant when) {
        if (userId == null) return;
        long now = System.currentTimeMillis();
        Long prev = lastWrite.putIfAbsent(userId, now);
        if (prev != null) {
            if (now - prev < throttleMs) {
                log.debug("lastSeen: throttle hit for user={}, lastWriteAgoMs={}", userId, now - prev);
                return;
            } else {
                lastWrite.remove(userId);
            }
        }
        lastWrite.put(userId, now);

        log.debug("lastSeen: scheduling update for user={}, when={}", userId, when);
        executor.execute(() -> {
            try {
                txTemplate.executeWithoutResult(status -> {
                    int updated = jdbc.update(
                        "INSERT INTO public.user_last_seen (user_id, last_seen) VALUES (?, ?) " +
                            "ON CONFLICT (user_id) DO UPDATE SET last_seen = GREATEST(EXCLUDED.last_seen, public.user_last_seen.last_seen)",
                        userId, Timestamp.from(when)
                    );
                    log.info("lastSeen (tx): rowsAffected={} for {}", updated, userId);
                });
            } catch (Exception e) {
                log.error("lastSeen: tx persist failed for user {}", userId, e);
            }
        });
    }

    // optional: helper to probe DB connectivity (call from a health endpoint temporarily)
    public int probeCount() {
        return jdbc.queryForObject("SELECT count(*) FROM public.user_last_seen", Integer.class);
    }
}
