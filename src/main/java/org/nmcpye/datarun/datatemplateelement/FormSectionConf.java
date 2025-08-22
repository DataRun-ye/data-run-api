package org.nmcpye.datarun.datatemplateelement;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FormSectionConf extends AbstractElement {

    private Boolean repeatable = false;
    private String categoryDataElementId;

    @JsonProperty
    public String getId() {
        return getName();
    }

    public void setCategoryDataElementId(String categoryDataElementId) {
        if (getRepeatable() == null || !getRepeatable() && categoryDataElementId == null) {
            // no-op, only in repeatable sections
            return;
        }

        this.categoryDataElementId = categoryDataElementId;
    }

    @Override
    public FormSectionConf path(String path) {
        this.setPath(path);
        return this;
    }
}
