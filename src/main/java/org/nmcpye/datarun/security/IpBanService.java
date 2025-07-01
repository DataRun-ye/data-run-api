package org.nmcpye.datarun.security;

import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author Hamza Assada, (7amza.it@gmail.com)
 */
@Service
public class IpBanService {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 5 * 60 * 1000;     // 5 minutes
    private static final long BAN_DURATION_MS = 30 * 60 * 1000; // 30 minutes

    private final ConcurrentHashMap<String, Deque<Long>> requestTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> bannedIps = new ConcurrentHashMap<>();

    public void registerSuspiciousActivity(String ip) {
        long now = System.currentTimeMillis();

        Deque<Long> deque = requestTimestamps.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        deque.add(now);

        // Remove timestamps older than window
        while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) {
            deque.pollFirst();
        }

        if (deque.size() > MAX_ATTEMPTS) {
            bannedIps.put(ip, now + BAN_DURATION_MS);
        }
    }

    public boolean isBanned(String ip) {
        Long expiry = bannedIps.get(ip);
        if (expiry == null) return false;

        if (System.currentTimeMillis() > expiry) {
            bannedIps.remove(ip);
            requestTimestamps.remove(ip);
            return false;
        }

        return true;
    }

    public void clear(String ip) {
        bannedIps.remove(ip);
        requestTimestamps.remove(ip);
    }
}
