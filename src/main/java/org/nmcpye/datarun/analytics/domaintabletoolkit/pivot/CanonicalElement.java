package org.nmcpye.datarun.analytics.domaintabletoolkit.pivot;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CanonicalElement {
    private String canonicalElementId;
    private String safeName;
    private String templateSlug;
    private String dataType;
    private String semanticType;
    private String parentRepeatId; // nullable
}
