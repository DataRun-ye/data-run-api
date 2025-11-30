package org.nmcpye.datarun.analytics.domaintabletoolkit.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PivotSpec {
    private String templateUid;
    private Instant from;
    private Instant to;
    private List<String> ceIds;      // canonical_element_id list
    private Map<String, Object> options; // optional: partitionBy, extra filters
    private String createdBy;
}
