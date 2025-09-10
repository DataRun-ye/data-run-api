package org.nmcpye.datarun.analytics.pivot;

import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDto;
import org.nmcpye.datarun.analytics.pivot.dto.PivotMetadataResponse;

import java.util.Optional;

/**
 * PivotMetadataService provides the front-end and backend code with the
 * list of queryable pivot fields available.
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
public interface PivotMetadataService {
    /**
     * Return cached metadata for the requested template and version.
     *
     * @param templateUid        template uid
     * @param templateVersionUid template version uid
     * @return PivotMetadataResponse available fields and hints
     */
    PivotMetadataResponse getMetadataForTemplate(String templateUid, String templateVersionUid);

    /**
     * Resolve an individual field by UID.
     *
     * @param standardizedId     standardized Uid
     * @param templateUid        template uid context
     * @param templateVersionUid template version uid
     * @return Optional containing the resolved PivotFieldDto if found
     */
    Optional<PivotFieldDto> resolveFieldById(String standardizedId, String templateUid, String templateVersionUid);
}
