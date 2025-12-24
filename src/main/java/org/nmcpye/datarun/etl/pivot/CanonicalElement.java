package org.nmcpye.datarun.etl.pivot;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.jdbc.core.RowMapper;

@Data
@AllArgsConstructor
public class CanonicalElement {
    private String canonicalElementId;
    private String safeName;
    private String templateSlug;
    private String dataType;
    private String semanticType;
    private String parentRepeatId; // nullable

    public static final RowMapper<CanonicalElement> MAPPER = (rs, rowNum) -> new CanonicalElement(
        rs.getString("id"),
        rs.getString("safe_name"),
        rs.getString("template_slug"),
        rs.getString("data_type"),
        rs.getString("semantic_type"),
        rs.getString("parent_repeat_id")
    );
}
