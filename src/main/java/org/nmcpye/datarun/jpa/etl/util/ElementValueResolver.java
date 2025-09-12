package org.nmcpye.datarun.jpa.etl.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;
import org.nmcpye.datarun.jpa.etl.exception.InvalidReferenceValueException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * value resolver based on element type.
 * resolves primitive and non-category references value types.
 * Also sets default aggregatable non-numeric types, for example:
 * <p>
 * Boolean Types sets:
 * <pre>
 *     <code>value_bool = `parsed rawValue`, and value_num = `1/0`</code>
 * </pre>
 * timestamps set:
 * <pre>
 *     <code>value_ts = `parsed rawValue`, and value_num = `epochSecond`;</code>
 * </pre>
 *
 * @author Hamza Assada
 * @since 21/08/2025
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ElementValueResolver {
    private final ReferenceResolver referenceResolver;

    public ElementDataValue.ElementDataValueBuilder resolveValue(Object rawValue,
                                                                 FormDataElementConf elementConf) {

        final ElementDataValue.ElementDataValueBuilder vBuilder = ElementDataValue.builder();
        if (rawValue == null) {
            return vBuilder.valueText(null);
        }

        if (elementConf.getType().isSystemReferenceType()) {
            try {
                final var resolved = referenceResolver
                    .resolveReference(rawValue, elementConf);
                // store id of referenced entity
                return vBuilder.valueRefUid(resolved.getUid());
            } catch (InvalidReferenceValueException e) {
                log.error(e.getMessage());
                return vBuilder.valueAsText("Unresolved reference:" + rawValue);
            }

        }

        if (elementConf.getType().isNumeric()) {
            return vBuilder.valueNum(new BigDecimal(rawValue.toString()));
        } else if (elementConf.getType().isBoolean()) {
            final var aBoolean = Boolean.parseBoolean(rawValue.toString());
            return vBuilder.valueBool(aBoolean);
        } else if (elementConf.getType().isDateTimeType() && rawValue instanceof String) {
            final var dateTimeValue = LocalDateTime.parse(rawValue.toString())
                .toInstant(ZoneOffset.UTC);
            return vBuilder
                .valueTs(dateTimeValue);
        } else {
            return vBuilder.valueText(rawValue.toString());
        }
    }
}
