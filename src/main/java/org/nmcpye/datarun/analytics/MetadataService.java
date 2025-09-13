package org.nmcpye.datarun.analytics;

import org.nmcpye.datarun.analytics.dto.MetadataResponse;
import org.nmcpye.datarun.analytics.dto.QueryableElement;

import java.util.Map;
import java.util.Optional;

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


    /**
     * Return cached metadata map for the requested template and version, keyed by id.
     *
     * @param templateUid        template uid
     * @param templateVersionUid template version uid
     * @return map of id -> queryableMetadata available
     */
    Map<String, QueryableElement> getMetadataMapForTemplate(String templateUid, String templateVersionUid);

    /**
     * Resolve an individual field by UID.
     *
     * @param standardizedId     standardized Uid
     * @param templateUid        template uid context
     * @param templateVersionUid template version uid
     * @return Optional containing the resolved QueryableElement if found
     */
    Optional<QueryableElement> resolveFieldById(String standardizedId, String templateUid, String templateVersionUid);
}
