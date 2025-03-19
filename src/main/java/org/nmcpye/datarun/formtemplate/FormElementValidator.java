package org.nmcpye.datarun.formtemplate;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Hamza, 19/03/2025
 */
public class FormElementValidator {

    private static void validateNoDuplicates(
        Collection<? extends FormElementConf> elements,
        Function<FormElementConf, String> propertyExtractor,
        String formTemplateUid,
        String elementType
    ) {
        Map<String, List<FormElementConf>> groupedElements = elements.stream()
            .collect(Collectors.groupingBy(propertyExtractor));

        List<String> errors = new ArrayList<>();
        groupedElements.forEach((key, elementsWithKey) -> {
            if (elementsWithKey.size() > 1) {
                String duplicateDetails = elementsWithKey.stream()
                    .map(e -> String.format(
                        "%s (path: %s)",
                        e.getName(),
                        e.getPath()
                    ))
                    .collect(Collectors.joining(", "));
                errors.add(String.format(
                    "Duplicate %s %s: %s",
                    elementType,
                    key,
                    duplicateDetails
                ));
            }
        });

        // Throw if any duplicates found
        if (!errors.isEmpty()) {
            throw new IllegalQueryException(new ErrorMessage(
                ErrorCode.E1108, elementType, formTemplateUid, String.join(", ", String.join("\n", errors))
            ));
        }
    }

    private static void validateNoDuplicates(Collection<? extends FormElementConf> elements, String formTemplateUid, String elementType) {
        Set<String> seenIds = new HashSet<>();
        List<String> duplicateIds = elements.stream()
            .map(FormElementConf::getId)
            .filter(id -> !seenIds.add(id)) // Collect duplicate IDs
            .toList();

        if (!duplicateIds.isEmpty()) {
            throw new IllegalQueryException(new ErrorMessage(
                ErrorCode.E1108, elementType, formTemplateUid, String.join(", ", duplicateIds)
            ));
        }
    }

    public static void validateForm(DataFormTemplate template) {
        validateNoDuplicates(
            template.getSections(),
            FormElementConf::getId,
            template.getUid(),
            "section"
        );

        // Validate fields by ID
        validateNoDuplicates(
            template.getFields(),
            FormElementConf::getId,
            template.getUid(),
            "field"
        );

//        // Optional: Validate across both fields and sections
//        List<FormElementConf> allElements = new ArrayList<>();
//        allElements.addAll(template.getFields());
//        allElements.addAll(template.getSections());
//        validateNoDuplicates(
//            allElements,
//            FormElementConf::getId,
//            "element"
//        );
    }
}
