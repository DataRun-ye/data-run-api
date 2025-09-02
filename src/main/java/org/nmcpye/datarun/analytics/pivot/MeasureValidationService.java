package org.nmcpye.datarun.analytics.pivot;


import org.nmcpye.datarun.analytics.pivot.dto.MeasureRequest;
import org.nmcpye.datarun.analytics.pivot.exception.InvalidMeasureException;
import org.nmcpye.datarun.analytics.pivot.model.ValidatedMeasure;

/**
 * MeasureValidationService validates a client MeasureRequest and converts it into a
 * {@link ValidatedMeasure} that the PivotQueryBuilder can consume.
 * <p>
 * Responsibilities:
 * - Resolve element identifiers provided by the client (template-level etc:<uid> or global de:<uid>)
 * using PivotMetadataService and DataElementRepository.
 * - Validate requested aggregation against allowed aggregationModes for the element.
 * - Determine the correct MV target field for aggregation (value_num, option_uid, value_ts, value_bool etc).
 * - Build element predicate (Condition) that scopes the aggregate (prefer etc_uid for etc:..., fall back to de_uid).
 * - Normalize alias (uniqueness / auto-rename policy or throw).
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
public interface MeasureValidationService {
    /**
     * Validate and convert a MeasureRequest into a ValidatedMeasure inside a template context.
     *
     * @param req                 client measure request (elementIdOrUid, aggregation, alias, distinct, optionId)
     * @param templateUid         template uid for template-mode resolution
     * @param templateVersionUid  template version uid for version-scoped resolution
     * @return ValidatedMeasure ready for query builder
     * @throws InvalidMeasureException on validation or resolution failure
     */
    ValidatedMeasure validate(MeasureRequest req, String templateUid, String templateVersionUid) throws InvalidMeasureException;
}
