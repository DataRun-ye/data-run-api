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
            WITH base AS (
            SELECT ov.id,
                   ov.uid,
                   ov.code,
                   ov.name,
                   TRIM(COALESCE(analytics.translation_value(COALESCE(ov.translations, '[]'::jsonb), 'en', 'name'), ov.name)) AS option_name_en,
                   TRIM(COALESCE(analytics.translation_value(COALESCE(ov.translations, '[]'::jsonb), 'ar', 'name'), ov.name)) AS option_name_ar
            From public.option_value ov
                JOIN public.option_set ops ON ops.id = ov.option_set_id
            WHERE ops.uid = :optSetUid
            )
            SELECT b.* FROM base b WHERE b.code = :t OR b.uid = :t OR b.name = :t OR b.option_name_en = :t OR b.option_name_ar = :t
            ORDER BY CASE WHEN code = :t THEN 1 WHEN uid = :t THEN 2 WHEN name = :t THEN 3 WHEN option_name_en = :t THEN 4 WHEN option_name_ar = :t THEN 5 ELSE 6 END
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
