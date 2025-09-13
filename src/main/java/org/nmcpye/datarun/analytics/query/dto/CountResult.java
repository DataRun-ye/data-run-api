package org.nmcpye.datarun.analytics.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author Hamza Assada
 * @since 13/09/2025
 */
@Builder
@Data
@AllArgsConstructor
public final class CountResult {
    private final long count;
    private final boolean approximate;
}
