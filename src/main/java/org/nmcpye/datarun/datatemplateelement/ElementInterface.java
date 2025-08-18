package org.nmcpye.datarun.datatemplateelement;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Hamza Assada 27/05/2025 (7amza.it@gmail.com)
 */
public interface ElementInterface extends Serializable {
    String getId();

    String getPath();

    String getParent();

    String getCode();

    String getName();

    String getDescription();

    Map<String, String> getLabel();

    List<DataFieldRule> getRules();

    Integer getOrder();

    ElementInterface path(String path);

    Map<String, Object> getProperties();
}
