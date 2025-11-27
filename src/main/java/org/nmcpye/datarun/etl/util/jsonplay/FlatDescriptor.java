package org.nmcpye.datarun.etl.util.jsonplay;

import java.util.ArrayList;
import java.util.List;

public class FlatDescriptor {
    public String descriptorId;
    public String version = "1";
    public List<Element> elements = new ArrayList<>();

    public static class Element {
        public String pointer;           // supports '*' wildcard token: "/items/*/sku"
        public Kind kind;                // PRIMITIVE | OBJECT | ARRAY
        public boolean isArray;          // true if this pointer refers to an array node
        public Access access;            // EXTRACT_VALUE | EXPLODE | FIRST_ELEMENT | EXTRACT_FROM_ELEMENTS
        public String note;
    }

    public enum Kind {PRIMITIVE, OBJECT, ARRAY}

    public enum Access {EXTRACT, EXPLODE, KEEP_JSON}
}
