package org.nmcpye.datarun.analytics.domaintabletoolkit.model;

import lombok.Builder;
import org.nmcpye.datarun.datatemplateelement.AggregationType;


/**
 * @param elementId
 * @param columnAlias
 * @param valueType
 * @param isMeasure
 * @param aggregationType
 * @param isCategory
 * @author Hamza Assada
 * @since 25/08/2025
 */
@Builder
public record ElementColumnDefinition(String elementId,
                                      String columnAlias,
                                      AnalyticValueType valueType,
                                      Boolean isMeasure,
                                      AggregationType aggregationType,
                                      String optionSetId,
                                      Boolean isCategory) {
}
