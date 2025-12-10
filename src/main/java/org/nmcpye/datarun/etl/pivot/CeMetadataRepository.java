package org.nmcpye.datarun.etl.pivot;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CeMetadataRepository {
    private final JdbcTemplate jdbc;

    private final RowMapper<CanonicalElement> mapper = new RowMapper<CanonicalElement>() {
        @Override
        public CanonicalElement mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CanonicalElement(
                rs.getString("id"),
                rs.getString("safe_name"),
                rs.getString("template_slug"),
                rs.getString("data_type"),
                rs.getString("semantic_type"),
                rs.getString("parent_repeat_id")
            );
        }
    };

    public List<CanonicalElement> getElementsForActivity(String activityId) {
//        Cache cache = cacheManager.getCache("ceMetadata");
//        if (cache != null) {
//            @SuppressWarnings("unchecked")
//            List<CanonicalElement> cached = cache.get(activityId);
//            if (cached != null) return cached;
//        }

        String sql = "SELECT ce.id, ce.safe_name, ce.data_type, ce.semantic_type, ce.parent_repeat_id " +
            "FROM public.canonical_element ce JOIN analytics.dim_data_template dt ON ce.template_uid = dt.template_uid " +
            "WHERE dt.activity_id = ? ORDER BY dt.template_slug, ce.safe_name";
        List<CanonicalElement> ces = jdbc.query(sql, new Object[]{activityId}, mapper);

//        if (cache != null) cache.put(activityId, ces);
        return ces;
    }

    // New: fetch CEs for a specific template_uid
    public List<CanonicalElement> getElementsForTemplate(String templateUid) {
        String sql = "SELECT id, safe_name, NULL AS template_slug, data_type, semantic_type, parent_repeat_id "
            + "FROM public.canonical_element WHERE template_uid = ? ORDER BY safe_name";
        List<CanonicalElement> ces = jdbc.query(sql, new Object[]{templateUid}, mapper);
        return ces;
    }
}
