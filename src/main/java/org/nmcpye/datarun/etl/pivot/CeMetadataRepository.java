package org.nmcpye.datarun.etl.pivot;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CeMetadataRepository {
    private final JdbcTemplate jdbc;



    // New: fetch CEs for a specific template_uid
    public List<CanonicalElement> getElementsForTemplate(String templateUid) {
        String sql = "SELECT id, safe_name, NULL AS template_slug, data_type, semantic_type, parent_repeat_id "
            + "FROM public.canonical_element WHERE template_uid = ? ORDER BY safe_name";
        List<CanonicalElement> ces = jdbc.query(sql, new Object[]{templateUid}, CanonicalElement.MAPPER);
        return ces;
    }

    //---------------------------
    public List<CanonicalElementWithConfig> getElementsForTemplateWithConfig(String templateUid) {
        String sql = """
            SELECT
              ce.id,
              ce.safe_name,
              NULL AS template_slug,
              ce.data_type,
              ce.semantic_type,
              ce.parent_repeat_id,
              cec.safe_name       AS safe_name_override,
              cec.inherit_strategy,
              COALESCE(cec.explode, false) AS explode,
              COALESCE(cec.include_in_context, false) AS include_in_context,
              COALESCE(cec.completeness_weight, 1) AS completeness_weight,
              COALESCE(cec.index_hint, false) AS index_hint
            FROM public.canonical_element ce
            LEFT JOIN analytics.canonical_element_config cec
              ON cec.canonical_element_id = ce.id
            WHERE ce.template_uid = ?
            ORDER BY COALESCE(cec.safe_name, ce.safe_name)
            """;

        return jdbc.query(sql, new Object[]{templateUid}, CanonicalElementWithConfig.MAPPER);
    }
}
