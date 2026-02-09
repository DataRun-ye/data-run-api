package org.nmcpye.datarun.etl.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DimOptionJdbcRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public Optional<Map<String,Object>> findByOptionSetAndToken(String optionSetUid, String token) {
        if (optionSetUid == null || token == null) return Optional.empty();

        String sql = """
            SELECT ov.*
            FROM public.option_value ov
            JOIN public.option_set ops ON ops.id = ov.option_set_id
            WHERE ops.uid = :optSetUid AND (ov.code = :t OR ov.uid = :t OR ov.name = :t)
            ORDER BY CASE WHEN ov.code = :t THEN 1 WHEN ov.uid = :t THEN 2 ELSE 3 END
            LIMIT 1
            """;
        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("optSetUid", optionSetUid)
            .addValue("t", token);
        var rows = jdbc.queryForList(sql, p);
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(rows.get(0));
    }
}
