package org.nmcpye.datarun.mongo.domain.dataelement;

public class FormSectionConf extends FormElementConf {

    private Boolean repeatable;

    @Override
    public String getId() {
        return this.getName();
    }

    @Override
    public void setId(String id) {
        this.setName(id);
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(Boolean repeatable) {
        this.repeatable = repeatable;
    }
}
