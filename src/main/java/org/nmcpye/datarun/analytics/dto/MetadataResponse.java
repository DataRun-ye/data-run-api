package org.nmcpye.datarun.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author Hamza Assada
 * @since 27/08/2025
 */
/**
 * The unified metadata contract served to the client. It provides a single,
 * consistent list of all fields available for querying for a given template.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataResponse {
    /**
     * A single, unified list of all queryable fields, including both
     * system-level dimensions (e.g., team, org unit) and template-specific
     * fields (form questions).
     */
    private List<QueryableElement> availableFields;

    /**
     * Contextual information about the metadata, such as the template UID.
     */
    private Map<String, Object> hints;
}
