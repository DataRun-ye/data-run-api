package org.nmcpye.datarun.common;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DisplayProperty {
    @JsonProperty("name")
    NAME("name"),

    @JsonProperty("shortName")
    SHORTNAME("shortName");

    private String display;

    DisplayProperty(String display) {
        this.display = display;
    }

    @Override
    public String toString() {
        return display;
    }
}
