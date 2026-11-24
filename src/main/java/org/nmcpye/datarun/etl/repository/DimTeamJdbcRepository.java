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

    public Optional<Map<String,Object>> findByUidOrId(String id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM public.team WHERE uid = :id OR id = :id LIMIT 1";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("id", id);
        var rows = jdbc.queryForList(sql, p);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }
}

