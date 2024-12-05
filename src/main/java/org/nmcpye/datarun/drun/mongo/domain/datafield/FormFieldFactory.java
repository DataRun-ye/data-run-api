package org.nmcpye.datarun.drun.mongo.domain.datafield;

import org.nmcpye.datarun.drun.mongo.domain.enumeration.ValueType;

public class FormFieldFactory {
//    public static DataField createDataField(ValueType valueType, Map<String, Object> rawProperties) {
//        DataField dataField = new DataField();
//        dataField.setType(valueType);
//        FieldProperties properties = mapToProperties(valueType, rawProperties);
//        dataField.setProperties(properties);
//        return dataField;
//    }
//
//    private static FieldProperties mapToProperties(ValueType valueType, Map<String, Object> rawProperties) {
////        try {
////            Class<? extends FieldProperties> clazz = getPropertyClass(valueType);
////            ObjectMapper mapper = new ObjectMapper();
////            return mapper.convertValue(rawProperties, clazz);
////        } catch (Exception e) {
////            throw new IllegalArgumentException("Invalid properties for ValueType " + valueType, e);
////        }
//    }

    public static Class<? extends AbstractField> getPropertyClass(ValueType valueType) {
        return switch (valueType) {
            case SelectMulti, SelectOne -> OptionField.class;
            case RepeatableSection -> Repeat.class;
            case Section -> Section.class;
            case ScannedCode -> ScannedCodeField.class;
            case Reference -> ResourceField.class;
            default -> DefaultField.class;
        };
    }
}
