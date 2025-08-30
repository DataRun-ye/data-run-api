package org.nmcpye.datarun.jpa.datatemplate.elementmapping;

import lombok.Builder;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

/**
 * Lightweight metadata returned by DataElementService to keep TemplateFieldGenerator decoupled.
 *
 * @param valueType your enum
 * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
 */
@Builder
public record DataElementMeta(String elementUid, ValueType valueType, boolean isReference, String referenceTable) {
}
