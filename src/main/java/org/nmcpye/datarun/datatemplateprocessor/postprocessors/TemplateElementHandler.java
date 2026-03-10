package org.nmcpye.datarun.datatemplateprocessor.postprocessors;

import org.nmcpye.datarun.datatemplateelement.AbstractElement;

/**
 * @author Hamza Assada 18/03/2025 (7amza.it@gmail.com)
 */
public interface TemplateElementHandler<T extends AbstractElement> {
    TemplateElementHandler<T> linkWith(TemplateElementHandler<T> next);

    T process(T element);
}
