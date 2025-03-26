package org.nmcpye.datarun.formtemplate.validation.validators;

import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Hamza, 25/03/2025
 */
public class ElementReferenceValidator implements ExpressionValidator {

    private static final Pattern ELEMENT_REF_PATTERN = Pattern.compile("#\\{(\\w+)}");

    @Override
    public ElementValidationResult validate(String expression, FormWithFields template) {
        List<ElementExpressionValidationError> errors = new ArrayList<>();

        // Extract element references from the expression.
        Matcher matcher = ELEMENT_REF_PATTERN.matcher(expression);
        Set<String> referencedElements = new HashSet<>();
        while (matcher.find()) {
            referencedElements.add(matcher.group(1));
        }

        // Build a set of valid element names from the template.
        // Assume DataFormTemplate.getFields() returns a List<Field> where Field has a getName() method.
        Set<String> validElementNames = template.getFieldsConf().stream()
            .map(FormElementConf::getName)
            .collect(Collectors.toSet());

        // add section names when they are valid references:
        // validElementNames.addAll(template.getSections().stream().map(Section::getName).collect(Collectors.toSet()));

        // Check that each referenced element exists in the template.
        for (String ref : referencedElements) {
            if (!validElementNames.contains(ref)) {
                errors.add(
                    new ElementExpressionValidationError(
                        expression,
                        ref,
                        "Referenced element '%s' does not exist in the form template".formatted(ref)
                    )
                );
            }
        }

        return errors.isEmpty() ? ElementValidationResult.valid() : ElementValidationResult.invalid(errors);
    }
}
