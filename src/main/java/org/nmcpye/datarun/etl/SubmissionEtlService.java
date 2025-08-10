//package org.nmcpye.datarun.etl;
//
//import org.nmcpye.datarun.datatemplateelement.AbstractElement;
//import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
//import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
//import org.nmcpye.datarun.jpa.datavalue.DataValue;
//import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
///**
// * @author Hamza Assada 10/08/2025 (7amza.it@gmail.com)
// */
//@Service
//public class SubmissionEtlService {
//
//    // Main entry point
//    public List<DataValue> processSubmission(DataTemplateInstanceDto template,
//                                             DataFormSubmission submission) {
//        List<DataValue> normalizedRows = new ArrayList<>();
//        Map<String, AbstractElement> elementPathMap = template.getElementsAsPathMap(); // e.g., "adult.name" -> FormElement
//
//        // Recursive function to walk through the data
//        extractValues(
//            submission.getFormData(), // The current JSON data node (starts as root)
//            elementPathMap,
//            template.getRootPath(), // The current path context (starts as root)
//            normalizedRows,
//            null, // initial repeat_id
//            null  // initial repeat_index
//        );
//        return normalizedRows;
//    }
//
//    private void extractValues(Map<String, Object> currentDataNode, Map<String, AbstractElement> elementPathMap,
//                               String currentPath, List<SubmissionValue> normalizedRows,
//                               UUID repeatId, Integer repeatIndex) {
//
//        for (String key : currentDataNode.keySet()) {
//            String childPath = currentPath + "." + key;
//            Object value = currentDataNode.get(key);
//
//            AbstractElement element = elementPathMap.get(childPath);
//            if (element == null) continue; // No corresponding element in template, skip
//
//            // Check if this path corresponds to a repeatable section
//            if (element instanceof FormSectionConf && ((FormSectionConf) element).getRepeatable() && value instanceof List) {
//                List<Map<String, Object>> repeatedItems = (List<Map<String, Object>>) value;
//                int index = 0;
//                for (Map<String, Object> item : repeatedItems) {
//                    String repeatInstanceUid = (String) item.get("_uid");
//                    if (repeatInstanceUid == null) {
//                        // This is a data integrity issue. The client MUST send a UID.
//                        // Log an error, throw an exception, or skip this item based on business rules.
//                        throw new IllegalArgumentException("Missing _uid for repeatable item in submission " + submission.getId());
//                    }
//
//
//                    // --- IMPORTANT: Category ID extraction ---
//                    String categoryIdValue = extractCategoryId(item, childPath, elementPathMap);
//
//                    // Recurse into the repeatable item's children
//                    extractValues(item, elementPathMap, childPath, normalizedRows,
//                        newRepeatId, index, categoryIdValue);
//                    index++;
//                }
//            } else if (value instanceof Map) {
//                // Not repeatable, just a regular section group, recurse deeper
//                extractValues((Map<String, Object>) value, elementPathMap, childPath, normalizedRows,
//                    repeatId, repeatIndex, categoryId);
//            } else {
//                // Base case: This is a primitive value to be saved
//                DataValue newRow = new DataValue();
//                // ... set submission_id, form_template_id, etc.
//                newRow.setElementId(element.getId());
//                newRow.setPath(childPath);
//                newRow.setRepeatId(repeatId);
//                newRow.setRepeatIndex(repeatIndex);
//                newRow.setCategoryId(categoryId); // Set the captured category ID
//
//                // Type conversion logic
//                if (value instanceof Number) newRow.setValueNum((Number) value);
//                else if (value instanceof Boolean) newRow.setValueBool((Boolean) value);
//                else newRow.setValueText(value.toString());
//
//                normalizedRows.add(newRow);
//            }
//        }
//    }
//
//    private String extractCategoryId(Map<String, Object> repeatItemData, String sectionPath, Map<String, AbstractElement> elementPathMap) {
//        // Find the element within this section configured as the 'category'
//        AbstractElement categoryElement = findCategoryElementForSection(sectionPath);
//        if (categoryElement != null) {
//            // The path of the category element is relative to the section start
//            String relativePath = categoryElement.getPath().substring(sectionPath.length() + 1);
//            Object categoryValue = repeatItemData.get(relativePath);
//            return categoryValue != null ? categoryValue.toString() : null;
//        }
//        return null;
//    }
//}
