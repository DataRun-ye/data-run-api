package org.nmcpye.datarun.formtemplate.validation;

import org.nmcpye.datarun.formtemplate.validation.validators.ElementGeneralValidationError;
import org.nmcpye.datarun.formtemplate.validation.validators.FormValidationError;
import org.nmcpye.datarun.formtemplate.validation.validators.FormValidationResult;
import org.nmcpye.datarun.formtemplate.validation.validators.TemplateValidator;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada, 19/03/2025
 */
@Component
public class DuplicateElementValidator implements TemplateValidator {

    private static List<FormValidationError> validateNoDuplicates(
        Collection<? extends FormElementConf> elements,
        Function<FormElementConf, String> propertyExtractor,
        String formTemplateUid,
        String elementType
    ) {
        Map<String, List<FormElementConf>> groupedElements = elements.stream()
            .collect(Collectors.groupingBy(propertyExtractor));

        List<FormValidationError> errors = new ArrayList<>();
        groupedElements.forEach((key, elementsWithKey) -> {
            if (elementsWithKey.size() > 1) {
                List<String> duplicateDetails = elementsWithKey.stream()
                    .map(e -> String.format(
                        "%s (path: %s)",
                        e.getName(),
                        e.getPath()
                    ))
                    .toList();
                errors.add(new FormValidationError(key, String.format(
                    "'%s':%s)",
                    formTemplateUid,
                    elementType
                ), new ElementGeneralValidationError(String.format(
                    "Duplicate : %s",
                    duplicateDetails
                ))));
            }
        });
        return errors;
    }

    public FormValidationResult validate(FormWithFields template) {
        final var sectionsErrors = validateNoDuplicates(
            template.getSections(),
            FormElementConf::getId,
            template.getUid(),
            "section"
        );

        // Validate fields by ID
        final var fieldErrors = validateNoDuplicates(
            template.getFields(),
            FormElementConf::getId,
            template.getUid(),
            "field"
        );
        final List<FormValidationError> errors = new ArrayList<>();

        if (!sectionsErrors.isEmpty()) {
            errors.addAll(sectionsErrors);
        }
        if (!fieldErrors.isEmpty()) {
            errors.addAll(fieldErrors);
        }

        if (!errors.isEmpty()) {
            return FormValidationResult.invalid(errors);
        }
        return FormValidationResult.valid();
    }

}
