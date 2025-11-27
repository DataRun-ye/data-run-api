package org.nmcpye.datarun.etl.util;

import java.util.*;

/**
 * Flat descriptor: a top-level container that holds a list of ElementDescriptor
 * Each ElementDescriptor is independent and refers to an absolute JSON Pointer.
 */
public class FlatJsonDescriptor {
    public List<ElementDescriptor> elements = new ArrayList<>();

    public Optional<ElementDescriptor> find(String pointer) {
        return elements.stream().filter(e -> Objects.equals(e.pointer, pointer)).findFirst();
    }

    public static class ElementDescriptor {
        public String pointer;            // JSON Pointer, absolute: "/customer/phones/0/number"
        public NodeKind kind;             // PRIMITIVE | OBJECT | ARRAY
        public ElementKind elementKind;   // for arrays: PRIMITIVE | OBJECT | MIXED | UNKNOWN
        public Access access;             // EXTRACT_VALUE | FIRST_ELEMENT | EXPLODE | KEEP_JSON
        public boolean nullable = false;  // explicit null seen in samples
        public boolean optional = true;   // missing in some samples; true = not always present
        public String sampleValue;        // short sample for primitives (truncated)
        public Set<FieldType> sampleTypes = new LinkedHashSet<>(); // observed types
        public Integer arraySampleSize;   // if array at pointer, observed length (sample)
        public String parentPointer;      // nearest container pointer ("" for root)

        // convenience enums
        public enum NodeKind { PRIMITIVE, OBJECT, ARRAY }
        public enum ElementKind { PRIMITIVE, OBJECT, MIXED, UNKNOWN }
        public enum Access { EXTRACT_VALUE, FIRST_ELEMENT, EXPLODE, KEEP_JSON, INFER }
        public enum FieldType { STRING, NUMBER, BOOLEAN, OBJECT, ARRAY, NULL, MIXED }
        public enum IndexHint { NONE, BTREE, GIN, TEXT }
    }
}
