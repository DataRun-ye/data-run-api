package org.nmcpye.datarun.drun.postgres.common.translation;

/**
 * This enum defined a translatable property. The value of this enum is used for
 * mapping this translation with the object property name. The capitol Enum
 * string is used as a key when storing translation in Jsonb format.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public enum TranslationProperty {
    NAME("name"),
    SHORT_NAME("shortName"),
    DESCRIPTION("description"),
    FORM_NAME("formName"),
    NUMERATOR_DESCRIPTION("numeratorDescription"),
    DENOMINATOR_DESCRIPTION("denominatorDescription"),
    RELATIONSHIP_FROM_TO_NAME("fromToName"),
    RELATIONSHIP_TO_FROM_NAME("toFromName"),
    INSTRUCTION("instruction"),
    INCIDENT_DATE_LABEL("incidentDateLabel"),
    ENROLLMENT_DATE_LABEL("enrollmentDateLabel"),
    EXECUTION_DATE_LABEL("executionDateLabel"),
    DUE_DATE_LABEL("dueDateLabel"),
    CONTENT("content");

    private String name;

    TranslationProperty(String name) {
        this.name = name;
    }

    public static TranslationProperty fromValue(String value) {
        for (TranslationProperty type : TranslationProperty.values()) {
            if (type.getName().equalsIgnoreCase(value)) {
                return type;
            }
        }

        return null;
    }

    public String getName() {
        return name;
    }
}
