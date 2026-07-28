package org.nmcpye.datarun.jpa.datatemplate;

import lombok.Getter;
import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;

import java.util.Map;

/**
 * Immutable runtime view of one published template version.
 */
@Getter
public final class TemplateVersionContext {
    private final DataTemplateInstanceDto template;
    private final Map<String, AbstractElement> elementsByPath;

    public TemplateVersionContext(DataTemplateInstanceDto template) {
        this.template = template;
        this.elementsByPath = Map.copyOf(template.getAllElementPathMap());
    }
}
