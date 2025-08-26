package org.nmcpye.datarun.analytics.domaintabletoolkit.model;

import lombok.Builder;

import java.util.List;

/**
 * A record: a clean, validated table wide generation blueprint.
 *
 * @param projectName  a domain that creates its own partition (e.g., "Health & Nutrition")
 * @param projectAlias (e.g., "health_nutrition")
 * @param elements     to generate table columns
 * @author Hamza Assada
 * @since 25/08/2025
 */
@Builder
public record ProjectAnalyticsMetadata(
        String projectName,
        String projectAlias,
        List<ElementColumnDefinition> elements
) {
}
