package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The shape of a stored value, which decides what kind of definition can hold it.
 */
enum StoredShape {
    /** An object: a plain section's members, or a field holding an object. */
    OBJECT,
    /** A non-empty array whose elements are all objects: a repeating section's rows. */
    ROWS,
    /** Anything else (text, a number, a boolean, an array holding a non-object): a field's value. */
    LEAF,
    /** An empty array: zero rows of a repeating section, or an empty multi-select. */
    ANY;

    static StoredShape of(JsonNode value) {
        if (value.isObject()) {
            return OBJECT;
        }
        if (value.isArray()) {
            if (value.isEmpty()) {
                return ANY;
            }
            for (JsonNode element : value) {
                if (!element.isObject()) {
                    return LEAF;
                }
            }
            return ROWS;
        }
        return LEAF;
    }

    /** Whether a definition of this kind can hold a value of this shape. */
    boolean heldBy(boolean field, boolean repeatingSection) {
        return switch (this) {
            case OBJECT -> field || !repeatingSection;
            case ROWS -> field || repeatingSection;
            case LEAF -> field;
            case ANY -> true;
        };
    }
}
