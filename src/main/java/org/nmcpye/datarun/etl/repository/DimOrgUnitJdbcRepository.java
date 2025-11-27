package org.nmcpye.datarun.etl.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DimOrgUnitJdbcRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public Optional<Map<String,Object>> findByUid(String orgUnitUid) {
        if (orgUnitUid == null) return Optional.empty();
        String sql = "SELECT * FROM analytics.dim_org_unit WHERE org_unit_uid = :uid LIMIT 1";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("uid", orgUnitUid);
        var rows = jdbc.queryForList(sql, p);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }

    public Optional<Map<String,Object>> findByCodeOrUid(String token) {
        if (token == null) return Optional.empty();
        String sql = "SELECT * FROM analytics.dim_org_unit "
            + "WHERE org_unit_uid = :t OR code = :t "
            + "ORDER BY CASE WHEN org_unit_uid = :t THEN 1 WHEN code = :t THEN 2 ELSE 3 END LIMIT 1";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("t", token);
        var rows = jdbc.queryForList(sql, p);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }
}

