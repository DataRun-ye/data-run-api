package org.nmcpye.datarun.analytics;


import org.nmcpye.datarun.analytics.dto.MeasureRequest;
import org.nmcpye.datarun.analytics.exception.InvalidMeasureException;
import org.nmcpye.datarun.analytics.model.ValidatedMeasure;

/**
 * MeasureValidationService validates a client MeasureRequest and converts it into a
 * {@link ValidatedMeasure} that the JooqQueryBuilder can consume.
 * <p>
 * Responsibilities:
 * - Resolve element identifiers provided by the client.
 * - Validate requested aggregation against allowed aggregationModes for the element.
 * - Determine the correct MV target field for aggregation (value_num, option_uid, value_ts, value_bool etc).
 * - Build element predicate (Condition) that scopes the aggregate
 * - Normalize alias (uniqueness / auto-rename policy or throw).
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
public interface MeasureValidationService {
    /**
     * Validate and convert a MeasureRequest into a ValidatedMeasure.
     */
    ValidatedMeasure validate(MeasureRequest req, String templateUid, String templateVersionUid) throws InvalidMeasureException;
}
