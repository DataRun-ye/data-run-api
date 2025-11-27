package org.nmcpye.datarun.etl.repository;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.dto.SubmissionKeyDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SubmissionKeysJdbcRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public void upsertSubmissionKey(String submissionUid, Long submissionSerial, String status, String submissionId,
                                    String assignmentUid, String activityUid, String orgUnitUid, String teamUid,
                                    String templateUid, Instant lastSeen) {
        if (submissionUid == null) throw new IllegalArgumentException("submissionUid required");
        String sql = "INSERT INTO analytics.submission_keys (submission_uid, submission_serial, status, submission_id, assignment_uid, activity_uid, org_unit_uid, team_uid, template_uid, last_seen, created_at, updated_at) "
            + "VALUES (:submissionUid, :submissionSerial, :status, :submissionId, :assignmentUid, :activityUid, :orgUnitUid, :teamUid, :templateUid, :lastSeen, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
            + "ON CONFLICT (submission_uid) DO UPDATE SET "
            + "submission_id = EXCLUDED.submission_id, "
            + "assignment_uid = EXCLUDED.assignment_uid, "
            + "activity_uid = EXCLUDED.activity_uid, "
            + "org_unit_uid = EXCLUDED.org_unit_uid, "
            + "team_uid = EXCLUDED.team_uid, "
            + "template_uid = EXCLUDED.template_uid, "
            + "last_seen = GREATEST(COALESCE(submission_keys.last_seen, to_timestamp(0)), COALESCE(EXCLUDED.last_seen, to_timestamp(0))), "
            + "updated_at = CURRENT_TIMESTAMP";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("submissionUid", submissionUid)
            .addValue("submissionSerial", submissionSerial)
            .addValue("status", status)
            .addValue("submissionId", submissionId)
            .addValue("assignmentUid", assignmentUid)
            .addValue("activityUid", activityUid)
            .addValue("orgUnitUid", orgUnitUid)
            .addValue("teamUid", teamUid)
            .addValue("templateUid", templateUid)
            .addValue("lastSeen", lastSeen != null ? Timestamp.from(lastSeen) : null);
        jdbc.update(sql, params);
    }

    public Optional<SubmissionKeyDto> findBySubmissionUid(String submissionUid) {
        if (submissionUid == null) return Optional.empty();
        String sql = "SELECT * FROM analytics.submission_keys WHERE submission_uid = :submissionUid LIMIT 1";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("submissionUid", submissionUid);
        List<SubmissionKeyDto> rows = jdbc.query(sql, params, SubmissionKeyDto.ROW_MAPPER);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }
}
