package org.nmcpye.datarun.drun.mongo.domain.datafield;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.core.mapping.Field;

public class OptionField extends DefaultField {
    @NotNull
    @Field("listName")
    private String listName;

    private String choiceFilter;

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public String getChoiceFilter() {
        return choiceFilter;
    }

    public void setChoiceFilter(String choiceFilter) {
        this.choiceFilter = choiceFilter;
    }
}
