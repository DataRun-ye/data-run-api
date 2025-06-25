package org.nmcpye.datarun.datatemplateelement;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FormSectionConf extends AbstractElement {

    private Boolean repeatable = false;

    @JsonProperty
    public String getId() {
        return getName();
    }

    @Override
    public FormSectionConf path(String path) {
        this.setPath(path);
        return this;
    }
}
