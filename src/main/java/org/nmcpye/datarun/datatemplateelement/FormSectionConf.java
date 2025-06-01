package org.nmcpye.datarun.datatemplateelement;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FormSectionConf extends AbstractElement {

    private Boolean repeatable = false;

    @Override
    public FormSectionConf path(String path) {
        this.setPath(path);
        return this;
    }
}
