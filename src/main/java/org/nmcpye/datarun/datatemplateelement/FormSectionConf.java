package org.nmcpye.datarun.datatemplateelement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class FormSectionConf extends AbstractElement implements Serializable {

    private Boolean repeatable = false;
    private String categoryId;

    @JsonProperty
    @JsonIgnore
    public String getId() {
        return getName();
    }

    /**
     * only a repeatable section can set a category
     *
     * @param categoryId the repeat's child element id to be used as a category for the repeat
     */
    public void setCategoryId(String categoryId) {
        if (getRepeatable() == null || !getRepeatable() && categoryId == null) {
            // no-op, only in repeatable sections
            return;
        }

        this.categoryId = categoryId;
    }

    @Override
    public FormSectionConf path(String path) {
        this.setPath(path);
        return this;
    }

    public FormSectionConf name(String name) {
        this.setName(name);
        return this;
    }
}
