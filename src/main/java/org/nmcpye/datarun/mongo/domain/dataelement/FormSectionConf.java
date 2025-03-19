package org.nmcpye.datarun.mongo.domain.dataelement;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FormSectionConf extends FormElementConf {

    private Boolean repeatable = false;

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public FormSectionConf path(String path) {
        this.setPath(path);
        return this;
    }
//    public FormSectionConf() {
//        if (getId() == null || getId().isEmpty()) {
//            setId(CodeGenerator.generateUid());
//        }
//    }
//
//    public FormElementConf id(String id) {
//        this.setName(id);
//        return this;
//    }
}
