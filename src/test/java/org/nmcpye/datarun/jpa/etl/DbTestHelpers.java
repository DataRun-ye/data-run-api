package org.nmcpye.datarun.jpa.etl;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * Small DB helpers for assertions. Keeping all SQL in one place.
 *
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
public final class DbTestHelpers {

    public static List<Map<String, Object>> fetchAllValues(JdbcTemplate jdbc, String submissionId) {
        return jdbc.queryForList("SELECT * FROM element_data_value WHERE submission_id = ?", submissionId);
    }

    public static List<String> fetchOptionIdsFor(JdbcTemplate jdbc, String submissionId, String elementId) {
        return jdbc.queryForList("SELECT option_id FROM element_data_value WHERE submission_id = ? AND element_id = ? AND deleted_at IS NULL", String.class, submissionId, elementId);
    }

    public static int countActiveValues(JdbcTemplate jdbc, String submissionId, String elementId) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM element_data_value WHERE submission_id = ? AND element_id = ? AND deleted_at IS NULL", Integer.class, submissionId, elementId);
        return c == null ? 0 : c;
    }

    public static void assertActiveOptionIds(JdbcTemplate jdbc, String submissionId, String elementId, String... expected) {
        List<String> ids = fetchOptionIdsFor(jdbc, submissionId, elementId);
        // simple assertion (use AssertJ or JUnit in tests)
        if (ids.size() != expected.length) {
            throw new AssertionError("expected " + expected.length + " ids but found " + ids);
        }
        for (String e : expected) {
            if (!ids.contains(e)) throw new AssertionError("missing expected id " + e + " found " + ids);
        }
    }
}
