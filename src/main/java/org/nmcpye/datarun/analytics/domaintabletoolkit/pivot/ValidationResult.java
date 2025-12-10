package org.nmcpye.datarun.analytics.domaintabletoolkit.pivot;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class ValidationResult {
    private boolean passed;
    private long factsRows;
    private long submissionCount;
    private double ratio;
    private String message;
}
