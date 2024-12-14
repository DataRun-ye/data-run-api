package org.nmcpye.datarun.mongo.domain;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class OptionSet implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull
    @Field("list_name")
    private String listName;

    @Field("options")
    private Set<DataOption> options = new HashSet<>();

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public Set<DataOption> getOptions() {
        return options;
    }

    public void setOptions(Set<DataOption> options) {
        this.options = options;
    }
}
