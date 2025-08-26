package org.nmcpye.datarun.jpa.pivot;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * Incoming request from frontend describing one measure.
 *
 * @author Hamza Assada
 * @since 26/08/2025
 */
@Builder
@Getter
public class PivotMeasureRequest implements Serializable {
    private final String elementId;
    private final Aggregation aggregation;
    private final String alias;     // optional alias
    private final Boolean distinct; // optional (for COUNT)
    private final String optionId;  // optional (for per-option counts)

    public enum Aggregation {
        SUM,
        AVG,
        MIN,
        MAX,
        COUNT,
        COUNT_DISTINCT,
        SUM_TRUE
    }
}
