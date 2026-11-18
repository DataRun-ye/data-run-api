package org.nmcpye.datarun.jpa.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records when each user was last seen and which app they used: the app
 * version a phone reports and the {@code referenceVersion} of the user's last
 * assignments sync (0 = apps before 6.0.2, which do not receive Reference
 * forms). Updates are buffered in memory and flushed in one batch.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LastSeenService {

    private static final String UPSERT = """
        INSERT INTO public.user_last_seen (user_id, last_seen, app_version, reference_version)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (user_id)
        DO UPDATE SET
            last_seen = GREATEST(EXCLUDED.last_seen, public.user_last_seen.last_seen),
            app_version = COALESCE(EXCLUDED.app_version, public.user_last_seen.app_version),
            reference_version = COALESCE(EXCLUDED.reference_version, public.user_last_seen.reference_version)
        """;
    private static final int[] UPSERT_TYPES = {Types.VARCHAR, Types.TIMESTAMP, Types.VARCHAR, Types.INTEGER};

    private final JdbcTemplate jdbc;

    // Buffer: user id -> what the latest requests said. Thread-safe.
    private final Map<String, Seen> pendingUpdates = new ConcurrentHashMap<>();

    /** In-memory only, so requests pay no database latency. */
    public void touch(String userId, Instant when) {
        touch(userId, when, null, null);
    }

    /**
     * In-memory only. A null app version or reference version means the request
     * did not say, so the stored value is kept.
     */
    public void touch(String userId, Instant when, String appVersion, Integer referenceVersion) {
        if (userId == null || when == null) {
            return;
        }
        pendingUpdates.merge(userId, new Seen(when, appVersion, referenceVersion), Seen::merge);
    }

    /** Runs every 2 minutes by default and flushes all pending updates in one batch. */
    @Scheduled(fixedRateString = "${application.last-seen.flush-interval:120000}")
    @Transactional
    public void flushBuffer() {
        if (pendingUpdates.isEmpty()) {
            return;
        }

        log.debug("Flushing last seen updates for {} users...", pendingUpdates.size());
        List<Object[]> batchArgs = new ArrayList<>();
        for (String userId : pendingUpdates.keySet()) {
            Seen seen = pendingUpdates.remove(userId); // atomic remove
            if (seen != null) {
                batchArgs.add(new Object[]{
                    userId,
                    Timestamp.from(seen.at()),
                    seen.appVersion(),
                    seen.referenceVersion()});
            }
        }
        if (batchArgs.isEmpty()) {
            return;
        }

        try {
            int[] results = jdbc.batchUpdate(UPSERT, batchArgs, UPSERT_TYPES);
            log.info("Batch updated last_seen for {} users.", results.length);
        } catch (Exception e) {
            // Dropping one cycle on error is acceptable for 'last seen'.
            log.error("Failed to flush last_seen batch", e);
        }
    }

    private record Seen(Instant at, String appVersion, Integer referenceVersion) {
        Seen merge(Seen newer) {
            return new Seen(
                newer.at().isAfter(at) ? newer.at() : at,
                newer.appVersion() != null ? newer.appVersion() : appVersion,
                newer.referenceVersion() != null ? newer.referenceVersion() : referenceVersion);
        }
    }
}
