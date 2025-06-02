//package org.nmcpye.datarun.attribute;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//
//import java.io.Serializable;
//import java.util.Objects;
//
//public class AttributeValue
//    implements Serializable {
//    private Attribute attribute;
//
//    private String value;
//
//    public AttributeValue() {
//    }
//
//    public AttributeValue(String value) {
//        this();
//        this.value = value;
//    }
//
//    public AttributeValue(String value, Attribute attribute) {
//        this.value = value;
//        this.attribute = attribute;
//    }
//
//    public AttributeValue(Attribute attribute, String value) {
//        this.value = value;
//        this.attribute = attribute;
//    }
//
//    @Override
//    public boolean equals(Object object) {
//        if (this == object) {
//            return true;
//        }
//
//        if (object == null || getClass() != object.getClass()) {
//            return false;
//        }
//
//        AttributeValue that = (AttributeValue) object;
//
//        if (!Objects.equals(attribute, that.attribute)) {
//            return false;
//        }
//
//        if (!Objects.equals(value, that.value)) {
//            return false;
//        }
//
//        return true;
//    }
//
//    @Override
//    public int hashCode() {
//        int result = 7;
//        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
//        result = 31 * result + (value != null ? value.hashCode() : 0);
//        return result;
//    }
//
//    @Override
//    public String toString() {
//        return "AttributeValue{" +
//            "class=" + getClass() +
//            ", value='" + value + '\'' +
//            ", attribute='" + attribute + '\'' +
//            '}';
//    }
//
//    @JsonProperty
//    public String getValue() {
//        return value;
//    }
//
//    public void setValue(String value) {
//        this.value = value;
//    }
//
//    @JsonProperty
//    public Attribute getAttribute() {
//        return attribute;
//    }
//
//    public void setAttribute(Attribute attribute) {
//        this.attribute = attribute;
//    }
//}
