package org.nmcpye.datarun.datatemplateelement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@JsonIgnoreProperties(value = {"path"}, allowGetters = true)
public abstract class AbstractElement implements ElementInterface {
    private String parent;
    @NotNull
    private String name;
    private String path;
    private String code;
    @Size(max = 2000)
    private String description;
    private Map<String, String> label;
    private List<DataFieldRule> rules = new ArrayList<>();
    private Integer order;

    @Override
    public AbstractElement path(String path) {
        setPath(path);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormSectionConf that)) return false;
        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
