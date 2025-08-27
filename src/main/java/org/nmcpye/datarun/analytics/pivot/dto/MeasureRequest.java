package org.nmcpye.datarun.analytics.pivot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Client-side measure request (template-mode).
 * elementIdOrUid: either etc:<id> (e.g. etc:123) or dataElement ULID (string)
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasureRequest {
    /**
     * element_id from element_template_config
     */
    private String elementIdOrUid;

    /**
     * SUM, AVG, COUNT, COUNT_DISTINCT, MIN, MAX, SUM_TRUE
     */
    private String aggregation;
    /**
     * optional alias for result column
     */
    private String alias;
    /**
     * Optional for COUNT, if true COUNT(DISTINCT ...)
     */
    private Boolean distinct;
    private String optionId;    // optional when counting per option (option ULID)
}
