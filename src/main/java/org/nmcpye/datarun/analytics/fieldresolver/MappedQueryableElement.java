package org.nmcpye.datarun.analytics.fieldresolver;

import lombok.Builder;

/**
 * A simple record to hold the parsed components of a standardized ID
 * the namespace:value parts
 *
 * @author Hamza Assada
 * @since 02/09/2025
 */
@Builder
public record MappedQueryableElement(String namespace, String value) {
    public static MappedQueryableElement from(String standardizedId) {
        if (standardizedId == null || !standardizedId.contains(":")) {
            // Handle legacy or non-namespaced IDs
            return new MappedQueryableElement("core", standardizedId); // Assume 'core' for backwards compatibility
        }
        String[] parts = standardizedId.split(":", 2);
        return new MappedQueryableElement(parts[0], parts[1]);
    }
}
