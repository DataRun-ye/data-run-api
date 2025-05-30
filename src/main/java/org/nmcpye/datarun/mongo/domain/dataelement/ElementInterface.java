package org.nmcpye.datarun.mongo.domain.dataelement;

import org.nmcpye.datarun.mongo.domain.DataFieldRule;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <27-05-2025>
 */
public interface ElementInterface<ID> extends Serializable {
    ID getId();

    String getPath();

    String getParent();

    String getCode();

    String getName();

    String getDescription();

    Map<String, String> getLabel();

    List<DataFieldRule> getRules();

    Integer getOrder();

    void setPath(String path);

    void setParent(String parent);

    void setCode(String code);

    void setName(String name);

    void setDescription(String description);

    void setLabel(Map<String, String> label);

    void setRules(List<DataFieldRule> rules);

    void setOrder(Integer order);

    default ElementInterface<ID> path(String path) {
        this.setPath(path);
        return this;
    }
}
