package org.nmcpye.datarun.mongo.domain.dataelement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.mongo.domain.DataFieldRule;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(value = {"path"}, allowGetters = true)
@Getter
@Setter
public abstract class FormElementConf implements Serializable {
    private static final long serialVersionUID = 1L;

    private String path;

    private String parent;

    @Field("code")
    private String code;

    @NotNull
    @Field("name")
    private String name;

    @Size(max = 2000)
    @Field("description")
    private String description;

    private Map<String, String> label;

    private List<DataFieldRule> rules;
    private List<String> appearance;

    private Integer order = 0;

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
