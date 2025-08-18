package org.nmcpye.datarun.datatemplateelement;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FormSectionConf extends AbstractElement {

    private Boolean repeatable = false;
    private String repeatCategoryElement;

    @JsonProperty
    public String getId() {
        return getName();
    }

    public void setRepeatCategoryElement(String repeatCategoryElement) {
        if (getRepeatable() == null || !getRepeatable() && repeatCategoryElement == null) {
            // no-op, only in repeatable sections
            return;
        }

        this.repeatCategoryElement = repeatCategoryElement;
    }

    @Override
    public FormSectionConf path(String path) {
        this.setPath(path);
        return this;
    }
}
