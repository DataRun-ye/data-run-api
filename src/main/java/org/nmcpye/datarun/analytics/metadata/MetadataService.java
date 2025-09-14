package org.nmcpye.datarun.analytics.metadata;

import org.nmcpye.datarun.analytics.dto.MetadataResponse;

/**
 * PivotMetadataService provides the front-end and backend code with the
 * list of queryable pivot fields available.
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
public interface MetadataService {
    /**
     * Return cached metadata for the requested template and version.
     *
     * @param templateUid        template uid
     * @param templateVersionUid template version uid
     * @return PivotMetadataResponse available fields
     */
    MetadataResponse getMetadataForTemplate(String templateUid, String templateVersionUid);
}
