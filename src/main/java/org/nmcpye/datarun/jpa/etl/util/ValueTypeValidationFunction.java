package org.nmcpye.datarun.jpa.etl.util;

import lombok.Builder;
import lombok.Getter;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;

import java.util.function.Function;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@Builder
@Getter
public class ValueTypeValidationFunction {
    private Function<Object, Boolean> function;

    private ValueType valueType;

    private String message;
}
