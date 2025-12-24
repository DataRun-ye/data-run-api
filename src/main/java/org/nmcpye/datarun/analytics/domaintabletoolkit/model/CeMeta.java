package org.nmcpye.datarun.analytics.domaintabletoolkit.model;

import lombok.Builder;
import org.nmcpye.datarun.jpa.datatemplate.DataType;
import org.nmcpye.datarun.jpa.datatemplate.SemanticType;


/**
 * @param elementId
 * @param columnAlias
 * @param dataType
 * @author Hamza Assada
 * @since 25/08/2025
 */
@Builder
public record CeMeta(String elementId,
                     String columnAlias,
                     DataType dataType,
                     SemanticType semanticType,
                     String templateUid,
                     String optionSetUid) {
}
