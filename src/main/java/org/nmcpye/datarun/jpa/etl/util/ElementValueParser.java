package org.nmcpye.datarun.jpa.etl.util;

import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author Hamza Assada 21/08/2025 (7amza.it@gmail.com)
 */
public class ElementValueParser {
    public static ElementDataValue.ElementDataValueBuilder parseValue(Object rawValue,
                                                                FormDataElementConf elementConf) {
        final ElementDataValue.ElementDataValueBuilder vBuilder = ElementDataValue.builder();
        if (rawValue == null) {
            return vBuilder.valueText(null);
        } else if (rawValue instanceof Number && elementConf.getType().isNumeric()) {
            return vBuilder.valueNum(new BigDecimal(rawValue.toString()));
        } else if (rawValue instanceof Boolean && elementConf.getType().isBoolean()) {
            return vBuilder.valueBool((Boolean) rawValue);
        } else if (rawValue instanceof String && elementConf.getType().isDateTimeType()) {
            return vBuilder.valueTs(Instant.parse(rawValue.toString()));
        } else {
            return vBuilder.valueText(rawValue.toString());
        }
    }
}
