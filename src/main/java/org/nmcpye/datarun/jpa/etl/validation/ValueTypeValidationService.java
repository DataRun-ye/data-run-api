package org.nmcpye.datarun.jpa.etl.validation;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.etl.util.DateUtils;
import org.nmcpye.datarun.jpa.etl.util.MathUtils;
import org.nmcpye.datarun.jpa.etl.util.ValueTypeValidationFunction;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
public class ValueTypeValidationService {
    private static final String VALUE_STRING = "Value";
    private final List<ValueTypeValidationFunction> valueTypeValidationFunctions;

    public ValueTypeValidationService(TeamRepository teamRepository,
                                      OrgUnitRepository orgUnitRepository) {
        valueTypeValidationFunctions = ImmutableList.of(
                ValueTypeValidationFunction.builder().valueType(ValueType.Number)
                        .function(v -> !MathUtils.isNumeric(v.toString()))
                        .message(" '%s' is not a valid numeric type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.Integer)
                        .function(v -> !MathUtils.isNumeric(v.toString()))
                        .message(" '%s' is not a valid numeric type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.IntegerPositive)
                        .function(v -> !MathUtils.isNumeric(v.toString()))
                        .message(" '%s' is not a valid numeric type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.IntegerNegative)
                        .function(v -> !MathUtils.isNumeric(v.toString()))
                        .message(" '%s' is not a valid numeric type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.IntegerZeroOrPositive)
                        .function(v -> !MathUtils.isNumeric(v.toString()))
                        .message(" '%s' is not a valid numeric type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.Percentage)
                        .function(v -> !MathUtils.isNumeric(v.toString()))
                        .message(" '%s' is not a valid numeric type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.Boolean)
                        .function(v -> !MathUtils.isBool(v.toString()))
                        .message(" '%s' is not a valid boolean type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.Date)
                        .function(v -> !DateUtils.dateIsValid(v.toString()))
                        .message(" '%s' is not a valid date type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.DateTime)
                        .function(v -> !DateUtils.dateTimeIsValid(v.toString()))
                        .message(" '%s' is not a valid datetime for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.Time)
                        .function(v -> !DateUtils.dateTimeIsValid(v.toString()))
                        .message(" '%s' is not a valid time for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.TrueOnly)
                        .function(v -> !"true".equals(v))
                        .message(" '%s' is not true (true-only type) for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.Team)
                        .function(v -> teamRepository.findByUid(v.toString()).isEmpty())
                        .message(" '%s' is not true (true-only type) for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.OrganisationUnit)
                        .function(v -> orgUnitRepository.findByUid(v.toString()).isEmpty())
                        .message(" '%s' is not true (true-only type) for element %s ")
                        .build());


    }

    @Transactional(readOnly = true)
    public String validateValueType(FormDataElementConf elementConf, Object value) {

        ValueType valueType = Optional.ofNullable(elementConf)
                .orElseThrow(() -> new IllegalArgumentException("tracked entity element is required"))
                .getType();


        ValueTypeValidationFunction function = valueTypeValidationFunctions.stream()
                .filter(f -> f.getValueType() == valueType)
                .filter(f -> f.getFunction().apply(value)).findFirst().orElse(null);

        return Optional.ofNullable(function)
                .map(f -> VALUE_STRING +
                        String.format(f.getMessage(), StringUtils.substring(value.toString(), 0, 30),
                                elementConf.getId()))
                .orElse(null);
    }
}
