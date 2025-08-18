package org.nmcpye.datarun.jpa.datasubmission.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.jpa.datasubmission.migration.postgre.MigrationError;
import org.nmcpye.datarun.jpa.datasubmission.migration.postgre.MigrationErrorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hamza Assada 16/08/2025 (7amza.it@gmail.com)
 */

@Service
public class MigrationErrorService {

    private final MigrationErrorRepository repository;
    private final ObjectMapper objectMapper;

    public MigrationErrorService(MigrationErrorRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordError(Object rawItem, String stage, Throwable t) {
        try {
            String sourceId = extractId(rawItem);
            String submissionUid = extractSubmissionUid(rawItem);
            var payload = objectMapper.convertValue(rawItem, JsonNode.class);
            String errorClass = t == null ? null : t.getClass().getName();
            String message = t == null ? null : truncate(t.getMessage(), 4000);
            MigrationError me = new MigrationError(sourceId, submissionUid, stage,
                errorClass, message, payload);
            repository.persist(me); // uses BaseJpaRepository.persist
        } catch (Exception e) {
            // best-effort: don't let error recording crash the batch step
            // log and swallow
            // use your logger
            System.err.println("Failed to persist migration error: " + e.getMessage());
        }
    }

    private String extractId(Object raw) {
        if (raw == null) return null;
        try {
            var clazz = raw.getClass();
            var f = clazz.getDeclaredField("id");
            f.setAccessible(true);
            Object val = f.get(raw);
            return val == null ? null : val.toString();
        } catch (Exception ignored) {
            try {
                var m = raw.getClass().getMethod("getId");
                Object val = m.invoke(raw);
                return val == null ? null : val.toString();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String extractSubmissionUid(Object raw) {
        if (raw == null) return null;
        try {
            var clazz = raw.getClass();
            var f = clazz.getDeclaredField("uid");
            f.setAccessible(true);
            Object val = f.get(raw);
            return val == null ? null : val.toString();
        } catch (Exception ignored) {
            try {
                var m = raw.getClass().getMethod("getUid");
                Object val = m.invoke(raw);
                return val == null ? null : val.toString();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }
}
