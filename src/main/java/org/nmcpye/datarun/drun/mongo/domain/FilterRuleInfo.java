package org.nmcpye.datarun.drun.mongo.domain;

import java.io.Serializable;
import java.util.List;

/**
 * A DataOption.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FilterRuleInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fieldToFilter;

    private List<String> optionsToHide;

    private List<String> optionsToShow;

    public List<String> getOptionsToShow() {
        return optionsToShow;
    }

    public void setOptionsToShow(List<String> optionsToShow) {
        this.optionsToShow = optionsToShow;
    }

    public List<String> getOptionsToHide() {
        return optionsToHide;
    }

    public void setOptionsToHide(List<String> optionsToHide) {
        this.optionsToHide = optionsToHide;
    }

    public String getFieldToFilter() {
        return fieldToFilter;
    }

    public void setFieldToFilter(String fieldToFilter) {
        this.fieldToFilter = fieldToFilter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FilterRuleInfo)) {
            return false;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FilterRuleInfo{" +
            ", fieldToFilter='" + getFieldToFilter() + "'" +
            ", optionsToHide='" + getOptionsToHide() + "'" +
            ", optionsToShow='" + getOptionsToShow() + "'" +
            "}";
    }
}
