package org.nmcpye.datarun.formfield.datafield;

import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;

public class FormFieldFactory {
    public static Class<? extends AbstractField> getPropertyClass(ValueType valueType) {
        return switch (valueType) {
            case SelectMulti, SelectOne -> OptionField.class;
            case RepeatableSection -> Repeat.class;
            case Section -> Section.class;
            case ScannedCode -> ScannedCodeField.class;
            case Reference -> ReferenceField.class;
            default -> DefaultField.class;
        };
    }
}
