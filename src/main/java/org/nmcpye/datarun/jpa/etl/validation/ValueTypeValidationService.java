package org.nmcpye.datarun.jpa.etl.validation;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.etl.util.DateUtils;
import org.nmcpye.datarun.jpa.etl.util.MathUtils;
import org.nmcpye.datarun.jpa.etl.util.ValueTypeValidationFunction;
import org.nmcpye.datarun.jpa.option.exception.InvalidOptionCodesException;
import org.nmcpye.datarun.jpa.option.service.OptionService;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@Component
public class ValueTypeValidationService {
    private static final String VALUE_STRING = "Value";
    private final List<ValueTypeValidationFunction> validators;

    public ValueTypeValidationService(TeamRepository teamRepository,
                                      OrgUnitRepository orgUnitRepository, OptionService optionService) {

        validators = ImmutableList.of(
                ValueTypeValidationFunction.builder().valueType(ValueType.Number)
                        .predicate(v -> !MathUtils.isNumeric(v))
                        .message(" '%s' is not a valid numeric type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.Integer)
                        .predicate(v -> !MathUtils.isNumeric(v))
                        .message(" '%s' is not a valid numeric type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.IntegerPositive)
                        .predicate(v -> !MathUtils.isPositiveInteger(v))
                        .message(" '%s' is not a valid numeric type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.IntegerNegative)
                        .predicate(v -> !MathUtils.isNegativeInteger(v))
                        .message(" '%s' is not a valid numeric type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.IntegerZeroOrPositive)
                        .predicate(v -> !MathUtils.isZeroOrPositiveInteger(v))
                        .message(" '%s' is not a valid numeric type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.Percentage)
                        .predicate(v -> !MathUtils.isPercentage(v))
                        .message(" '%s' is not a valid numeric type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.Boolean)
                        .predicate(v -> !MathUtils.isBool(v))
                        .message(" '%s' is not a valid boolean type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.Date)
                        .predicate(v -> !DateUtils.dateIsValid(v))
                        .message(" '%s' is not a valid date type for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.DateTime)
                        .predicate(v -> !DateUtils.dateTimeIsValid(v))
                        .message(" '%s' is not a valid datetime for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.Time)
                        .predicate(v -> !DateUtils.dateTimeIsValid(v))
                        .message(" '%s' is not a valid time for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.TrueOnly)
                        .predicate(v -> !"true".equals(v))
                        .message(" '%s' is not true (true-only type) for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.Team)
                        .predicate(v -> teamRepository.findByUid(v.toString().trim()).isEmpty())
                        .message(" '%s' is not true (true-only type) for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.SelectMulti)
                        .predicate(v -> {
                            // validate shape (list of codes)
                            // invalid shape -> validation fails
                            if (!(v instanceof List<?> list)) return true;
                            if (list.isEmpty()) return false; // empty list is valid (explicit clear)
                            // ensure all items are strings
                            if (!list.stream()
                                    .allMatch(x -> x == null || x instanceof String)) return true;
                            // perform option existence validation by attempting mapping
                            try {
                                // you might pass elementConf.getOptionSetId() here if available
                                optionService.validateAndMapOptionCodes(list.stream()
                                                .map(Object::toString).collect(Collectors.toSet()),
                                        null);
                                return false; // validation OK
                            } catch (InvalidOptionCodesException ex) {
                                return true; // validation fails
                            }
                        })
                        .message(" '%s' selected options not in the system for element %s ")
                        .build(),
                ValueTypeValidationFunction.builder().valueType(ValueType.OrganisationUnit)
                        .predicate(v -> orgUnitRepository.findByUid(v.toString().trim()).isEmpty())
                        .message(" '%s' is not true (true-only type) for element %s ")
                        .build());
    }


    public Optional<String> validateValueType(FormDataElementConf elementConf, Object value) {
        ValueType type = elementConf.getType();
        for (ValueTypeValidationFunction f : validators) {
            if (f.getValueType() != type) continue;
            if (f.getPredicate().test(value)) {
                String snippet = value == null ? "null" : StringUtils.substring(value.toString(), 0, 200);
                return Optional.of(String.format(f.getMessage(), snippet, elementConf.getId()));
            }
        }
        return Optional.empty();
    }
}
