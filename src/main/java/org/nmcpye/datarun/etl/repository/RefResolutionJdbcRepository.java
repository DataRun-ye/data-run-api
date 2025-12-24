package org.nmcpye.datarun.etl.repository;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.dto.RefResolutionDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefResolutionJdbcRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public Optional<RefResolutionDto> findLatestByRawAndType(String rawValue, String refType) {
        if (rawValue == null || refType == null) return Optional.empty();
        String sql = "SELECT ref_resolution_uid, raw_value, raw_source, ref_type, resolved_uid, confidence, resolved_at, replaced_by, notes, created_at, updated_at "
            + "FROM analytics.ref_resolution "
            + "WHERE raw_value = :rawValue AND ref_type = :refType "
            + "ORDER BY resolved_at DESC NULLS LAST LIMIT 1";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("rawValue", rawValue)
            .addValue("refType", refType);
        List<RefResolutionDto> rows = jdbc.query(sql, params, RefResolutionDto.ROW_MAPPER);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }

    public void insertResolution(String refResolutionUid, String rawValue, String rawSource,
                                 String refType, String resolvedUid, double confidence, Instant resolvedAt, String replacedBy, String notes) {
        String sql = "INSERT INTO analytics.ref_resolution (ref_resolution_uid, raw_value, raw_source, ref_type, resolved_uid, "
            + "confidence, resolved_at, replaced_by, notes, created_at, updated_at) "
            + "VALUES (:uid, :rawValue, :rawSource, :refType, :resolvedUid, "
            + ":confidence, :resolvedAt, :replacedBy, :notes, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("uid", refResolutionUid)
            .addValue("rawValue", rawValue)
            .addValue("rawSource", rawSource)
            .addValue("refType", refType)
            .addValue("resolvedUid", resolvedUid)
            .addValue("confidence", confidence)
            .addValue("resolvedAt", resolvedAt != null ? Timestamp.from(resolvedAt) : null)
            .addValue("replacedBy", replacedBy)
            .addValue("notes", notes);
        jdbc.update(sql, params);
    }

    public List<RefResolutionDto> findHistoryByRaw(String rawValue) {
        if (rawValue == null) return List.of();
        String sql = "SELECT * FROM analytics.ref_resolution WHERE raw_value = :rawValue ORDER BY resolved_at DESC NULLS LAST";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("rawValue", rawValue);
        return jdbc.query(sql, params, RefResolutionDto.ROW_MAPPER);
    }

    public List<RefResolutionDto> findByResolvedUid(String resolvedUid) {
        if (resolvedUid == null) return List.of();
        String sql = "SELECT * FROM analytics.ref_resolution WHERE resolved_uid = :resolvedUid ORDER BY resolved_at DESC NULLS LAST";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("resolvedUid", resolvedUid);
        return jdbc.query(sql, params, RefResolutionDto.ROW_MAPPER);
    }
}
