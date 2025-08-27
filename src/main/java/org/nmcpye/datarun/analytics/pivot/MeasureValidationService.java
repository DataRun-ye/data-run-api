package org.nmcpye.datarun.analytics.pivot;


import org.nmcpye.datarun.analytics.pivot.dto.MeasureRequest;
import org.nmcpye.datarun.analytics.pivot.exception.InvalidMeasureException;

/**
 * @author Hamza Assada
 * @since 27/08/2025
 */
public interface MeasureValidationService {
    /**
     * Validate and convert a MeasureRequest into a ValidatedMeasure inside a template context.
     *
     * @param req               request from UI
     * @param templateId        template ULID
     * @param templateVersionId template version id
     * @return validated measure ready for query builder
     * @throws InvalidMeasureException on validation error
     */
    ValidatedMeasure validate(MeasureRequest req, String templateId, String templateVersionId) throws InvalidMeasureException;
}
