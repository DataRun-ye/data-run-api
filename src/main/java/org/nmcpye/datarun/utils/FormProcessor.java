package org.nmcpye.datarun.utils;

import org.nmcpye.datarun.drun.postgres.domain.enumeration.AssignmentStatus;
import org.nmcpye.datarun.mongo.domain.datafield.AbstractField;
import org.nmcpye.datarun.mongo.domain.datafield.Repeat;
import org.nmcpye.datarun.mongo.domain.datafield.Section;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormProcessor {
    static public Map<String, Object> extractValues(Map<String, Object> formData, DataFormTemplate formTemplate) {
        Map<String, Object> extractedValues = new HashMap<>();

//        extract(formData, formTemplate.getFields(), extractedValues);

        return extractedValues;
    }

    static private void extract(Map<String, Object> data, List<AbstractField> fields, Map<String, Object> extractedValues) {
        for (AbstractField field : fields) {
            if (field instanceof Repeat repeat && data.containsKey(field.getName())) {
                //
            } else if (field instanceof Section section && data.containsKey(field.getName())) {
                extract((Map<String, Object>) data.get(field.getName()), section.getFields(), extractedValues);
            } else if (field.getType() == ValueType.Progress && data.containsKey(field.getName())) {
                extractedValues.put(field.getName(), Arrays.stream(AssignmentStatus.values())
                    .filter(t -> t.name().equals(data.get(field.getName())))
                    .findFirst().orElse(null));
            } else if (data.containsKey(field.getName())) {
                extractedValues.put(field.getName(), data.get(field.getName()));
            }
        }
    }
}
