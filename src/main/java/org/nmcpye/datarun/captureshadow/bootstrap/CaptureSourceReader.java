package org.nmcpye.datarun.captureshadow.bootstrap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

@Component
final class CaptureSourceReader {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final String SELECT_COLUMNS = """
        id,
        serial_number,
        uid,
        deleted,
        deleted_at,
        form_data::text AS form_data,
        status,
        template_uid,
        template_version_uid,
        template_version_no,
        assignment_uid,
        team_uid,
        team_code,
        org_unit_uid,
        org_unit_code,
        org_unit_name,
        activity_uid,
        start_entry_time,
        finished_entry_time,
        created_by,
        created_date,
        last_modified_by,
        last_modified_date
        """;

    private final JdbcTemplate jdbc;

    CaptureSourceReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<CaptureSourceRow> readPage(long afterSerial, Long maximumSerial, int limit) {
        if (maximumSerial == null) {
            return List.of();
        }
        return jdbc.query(
            "SELECT " + SELECT_COLUMNS + " FROM data_submission "
                + "WHERE serial_number > ? AND serial_number <= ? "
                + "ORDER BY serial_number LIMIT ?",
            this::mapRow,
            afterSerial,
            maximumSerial,
            limit
        );
    }

    long count() {
        Long count = jdbc.queryForObject("SELECT count(*) FROM data_submission", Long.class);
        if (count == null) {
            throw new CaptureShadowBootstrapConflictException("Source count returned null");
        }
        return count;
    }

    Long maximumSerial() {
        return jdbc.queryForObject("SELECT max(serial_number) FROM data_submission", Long.class);
    }

    private CaptureSourceRow mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CaptureSourceRow(
            resultSet.getString("id"),
            resultSet.getLong("serial_number"),
            resultSet.getString("uid"),
            resultSet.getObject("deleted", Boolean.class),
            instant(resultSet, "deleted_at"),
            resultSet.getString("form_data"),
            resultSet.getString("status"),
            resultSet.getString("template_uid"),
            resultSet.getString("template_version_uid"),
            resultSet.getObject("template_version_no", Integer.class),
            resultSet.getString("assignment_uid"),
            resultSet.getString("team_uid"),
            resultSet.getString("team_code"),
            resultSet.getString("org_unit_uid"),
            resultSet.getString("org_unit_code"),
            resultSet.getString("org_unit_name"),
            resultSet.getString("activity_uid"),
            instant(resultSet, "start_entry_time"),
            instant(resultSet, "finished_entry_time"),
            resultSet.getString("created_by"),
            instant(resultSet, "created_date"),
            resultSet.getString("last_modified_by"),
            instant(resultSet, "last_modified_date")
        );
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column, Calendar.getInstance(UTC));
        return timestamp == null ? null : timestamp.toInstant();
    }
}
