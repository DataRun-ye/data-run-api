package org.nmcpye.datarun.query;

import org.nmcpye.datarun.analytics.dto.MeasureRequest;
import org.nmcpye.datarun.analytics.dto.QueryableElementMapping;
import org.nmcpye.datarun.analytics.metadata.MetadataResolver;

/**
 * QueryableElementMappingFactory - per-target factory to convert MeasureRequest
 * //    into QueryableElementMapping objects. This cleanly isolates mapping logic.
 *
 * @author Hamza Assada
 * @since 14/09/2025
 */

public interface QueryableElementMappingFactory {
    /**
     * Build a QueryableElementMapping for the given measure request using this target's schema.
     * <p>
     * The factory MUST produce: a type-safe Field<?>, an elementPredicate Condition
     * that scopes the aggregate (to be used with `.filterWhere(...)`), an alias and
     * any optionUid if applicable. Validation about allowed aggregations should be done
     * earlier, but factories may re-validate and throw expressive exceptions.
     */
    QueryableElementMapping build(MeasureRequest mreq, MetadataResolver resolver);
}
