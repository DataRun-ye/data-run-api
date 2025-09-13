package org.nmcpye.datarun.analytics.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Small hints object that the builder can set to guide execution decisions.
 *
 * @author Hamza Assada
 * @since 13/09/2025
 */
@Data
@Builder
@AllArgsConstructor
public class ExecutionHints {
    private boolean preferMaterializedView;
    private boolean canPushMatrixToDB;
    private Long estimatedRows;
}
