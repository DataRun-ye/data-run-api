package org.nmcpye.datarun.analytics.pivot;

import org.nmcpye.datarun.analytics.pivot.dto.PivotFieldDto;
import org.nmcpye.datarun.analytics.pivot.dto.PivotMetadataResponse;

import java.util.Optional;

/**
 * PivotMetadataService provides the front-end and backend code with the
 * list of available pivot fields for a given template (template-mode-first).
 * <p>
 * Responsibilities:
 * - Load ElementTemplateConfig rows for a template+version and enrich them with DataElement metadata.
 * - Produce a PivotMetadataResponse containing:
 * * coreDimensions: stable system dimensions (team_uid, org_unit_uid, activity_uid, submission_completed_at)
 * * measures: list of PivotFieldDto (id="etc:<uid>", label, category, dataType, aggregationModes, factColumn)
 * - Provide a resolveFieldByUidOrId(uidOrId, templateUid, templateVersionUid) method that locates
 * a single PivotFieldDto for resolving a client field reference during validation.
 * - Cache responses per templateUid+templateVersionUid using @Cacheable (eviction handled on template publish).
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
     * @return PivotMetadataResponse containing coreDimensions, measures and hints
     */
    PivotMetadataResponse getMetadataForTemplate(String templateUid, String templateVersionUid);

    /**
     * Resolve an individual field by UID or id (accepts alias forms like "etc:<uid>" or raw uids).
     *
     * @param uidOrId            template-level id string such as "etc:<uid>" or "de:<uid>"
     * @param templateUid        template uid context
     * @param templateVersionUid template version uid
     * @return Optional containing the resolved PivotFieldDto if found
     */
    Optional<PivotFieldDto> resolveFieldByUidOrId(String uidOrId, String templateUid, String templateVersionUid);
}
