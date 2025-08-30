package org.nmcpye.datarun.analytics.pivot;

import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDto;
import org.nmcpye.datarun.analytics.pivot.dto.PivotMetadataResponse;

import java.util.Optional;

/**
 * @author Hamza Assada
 * @since 27/08/2025
 */
public interface PivotMetadataService {
    final String PIVOT_CACHE_NAME = "pivot.metadata";

    /**
     * Return metadata for a given template (template-mode).
     * The implementation should be cached by templateId + templateVersionId.
     */
    PivotMetadataResponse getMetadataForTemplate(String templateId, String templateVersionId);

    /**
     * Resolve a field by either UID or internal id within the context of the given template.
     * Returns Optional.empty() if not found.
     */
    Optional<PivotFieldDto> resolveFieldByUidOrId(String uidOrId, String templateId, String templateVersionId);
}
