package org.nmcpye.datarun.datatemplateelement;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Hamza Assada 27/05/2025 (7amza.it@gmail.com)
 */
public interface ElementInterface extends Serializable {
//    ID getId();

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

    ElementInterface path(String path);
}
