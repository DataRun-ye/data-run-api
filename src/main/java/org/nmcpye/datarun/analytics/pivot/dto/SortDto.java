package org.nmcpye.datarun.analytics.pivot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SortDto {
    private String fieldOrAlias;
    @Builder.Default
    private boolean desc = false;
}
