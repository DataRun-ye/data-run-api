package org.nmcpye.datarun.formoption;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * A DataOption.
 */
@Getter
@Setter
public class DataOption implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private String listName;

    @NotNull
    private String name;


    private Integer order;

    private Map<String, String> label;

    private String filterExpression;

    private Map<String, Object> properties;

    public DataOption name(String name) {
        this.setName(name);
        return this;
    }

    public DataOption listName(String listName) {
        this.setListName(listName);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataOption that = (DataOption) o;
        return listName.equals(that.listName) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listName, name);
    }

}
