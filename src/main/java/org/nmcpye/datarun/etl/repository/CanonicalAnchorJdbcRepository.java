package org.nmcpye.datarun.etl.repository;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.dto.CanonicalElementAnchorDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class CanonicalAnchorJdbcRepository {
    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Return a map keyed by canonical_element_id -> CanonicalElementAnchorDto.
     * Missing ids are simply absent from the map (caller should apply defaults).
     */
    public Map<String, CanonicalElementAnchorDto> findByCanonicalElementIds(Collection<String> ceIds) {
        if (ceIds == null || ceIds.isEmpty()) return Map.of();
        var uuids = ceIds.stream().map(UUID::fromString).toList();
        String sql = "SELECT canonical_element_id, anchor_allowed, anchor_priority, updated_by, updated_at "
            + "FROM canonical_element_anchor "
            + "WHERE canonical_element_id = ANY(:ids)";
        // or IN (:ids::uuid[])
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("ids", uuids.toArray(new UUID[0]));
        List<CanonicalElementAnchorDto> rows = jdbc.query(sql, params, CanonicalElementAnchorDto.ROW_MAPPER);
        Map<String, CanonicalElementAnchorDto> map = new HashMap<>();
        for (CanonicalElementAnchorDto d : rows) map.put(d.getCanonicalElementId(), d);
        return map;
    }
}
