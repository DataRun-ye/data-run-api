package org.nmcpye.datarun.jpa.etl.util;

import lombok.Builder;
import lombok.Getter;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

import java.util.function.Predicate;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@Builder
//@Getter
@Getter
//@AllArgsConstructor
public class ValueTypeValidationFunction {
    private final ValueType valueType;
    private final Predicate<Object> predicate;
    private final String message;
}
