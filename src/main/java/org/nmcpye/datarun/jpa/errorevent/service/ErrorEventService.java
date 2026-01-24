package org.nmcpye.datarun.jpa.errorevent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.errorevent.ErrorEvent;
import org.nmcpye.datarun.jpa.errorevent.repository.ErrorEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.NativeWebRequest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ErrorEventService {
    private final ErrorEventRepository repo;
    private final ObjectMapper objectMapper;

    @Async("errorLoggingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(Throwable ex, NativeWebRequest request,
                        HttpStatus status, String username, String ip, Map<String, Object> extra) {
        try {
            ErrorEvent ev = new ErrorEvent();
            ev.setOccurredAt(Instant.now());
            ev.setLevel("ERROR");
            ev.setStatus(status == null ? null : status.value());
            ev.setPath(request == null ? null : request.getNativeRequest(HttpServletRequest.class).getRequestURI());
            ev.setMethod(request == null ? null : request.getNativeRequest(HttpServletRequest.class).getMethod());
            ev.setUsername(username != null ? username : extractUsername());
            ev.setClientIp(ip != null ? ip : extractClientIp(request));
            ev.setMessage(truncate(ex.getMessage(), 2000));
            ev.setException(ex.getClass().getName());
            ev.setStacktrace(truncate(getStackTrace(ex), 4000));
            ev.setContextJson(toJsonSafe(extra));
            repo.persistAndFlush(ev);
        } catch (Exception e) {
            // Never rethrow — fallback to logging to file/stdout
            log.error("Failed to persist error event (logging fallback): {}", e.getMessage(), e);
        }
    }

    private String extractUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? String.valueOf(auth.getName()) : "anonymous";
    }

    private String extractClientIp(NativeWebRequest request) {
        if (request == null) return null;
        HttpServletRequest req = request.getNativeRequest(HttpServletRequest.class);
        String xf = req == null ? null : req.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isBlank()) ? xf.split(",")[0].trim() : (req == null ? null : req.getRemoteAddr());
    }

    private String getStackTrace(Throwable ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String toJsonSafe(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map == null ? Map.of() : map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
