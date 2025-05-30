package org.nmcpye.datarun.mongo.domain.dataelement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.mongo.domain.DataFieldRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(value = {"path"}, allowGetters = true)
@Getter
@Setter
public abstract class FormElementConf
    implements ElementInterface<String> {
    private String path;

    private String parent;

    private String code;

    @NotNull
    private String name;

    @Size(max = 2000)
    private String description;

    private Map<String, String> label;

    private List<DataFieldRule> rules = new ArrayList<>();

    private Integer order;

    private List<String> appearance;

    abstract public String getId();

    public FormElementConf path(String path) {
        setPath(path);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormElementConf that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
