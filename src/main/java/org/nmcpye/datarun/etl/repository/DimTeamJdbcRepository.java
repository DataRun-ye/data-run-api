package org.nmcpye.datarun.etl.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DimTeamJdbcRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public Optional<Map<String,Object>> findByUid(String teamUid) {
        if (teamUid == null) return Optional.empty();
        String sql = "SELECT * FROM analytics.dim_team WHERE team_uid = :uid LIMIT 1";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("uid", teamUid);
        var rows = jdbc.queryForList(sql, p);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }


    public Optional<Map<String,Object>> findByActivityAndCodeOrUid(String activityUid, String token) {
        if (activityUid == null || token == null) return Optional.empty();
        String sql = "SELECT * FROM analytics.dim_team "
            + "WHERE activity_uid = :activityUid AND (team_code = :t OR team_uid = :t) "
            + "ORDER BY CASE WHEN team_code = :t THEN 1 WHEN team_uid = :t THEN 2 ELSE 3 END "
            + "LIMIT 1";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("activityUid", activityUid)
            .addValue("t", token);
        var rows = jdbc.queryForList(sql, p);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }
}

