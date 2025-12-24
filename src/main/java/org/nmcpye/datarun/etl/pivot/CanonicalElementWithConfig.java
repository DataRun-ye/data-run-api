package org.nmcpye.datarun.etl.pivot;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CanonicalElementWithConfig {
    // base CE
    private String canonicalElementId;
    private String safeName;
    private String templateSlug;
    private String dataType;
    private String semanticType;
    private String parentRepeatId;

    // config (cec)
    private String safeNameOverride;
    private String inheritStrategy;      // 'nearest'|'topmost'|'none'
    private boolean explode;             // explode multiselect arrays into event_multiselect
    private boolean includeInContext;
    private BigDecimal completenessWeight;
    private boolean indexHint;

    public boolean isMultiSelect() {
        return dataType.equals("MultiSelectOption");
    }

    public final static RowMapper<CanonicalElementWithConfig> MAPPER = (rs, rowNum) ->
        new CanonicalElementWithConfig(
            rs.getString("id"),
            rs.getString("safe_name"),
            rs.getString("template_slug"),
            rs.getString("data_type"),
            rs.getString("semantic_type"),
            rs.getString("parent_repeat_id"),
            rs.getString("safe_name_override"),
            rs.getString("inherit_strategy"),
            rs.getBoolean("explode"),
            rs.getBoolean("include_in_context"),
            rs.getBigDecimal("completeness_weight"),
            rs.getBoolean("index_hint")
        );
}
