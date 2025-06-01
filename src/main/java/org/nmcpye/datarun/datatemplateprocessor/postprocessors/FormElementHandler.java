package org.nmcpye.datarun.datatemplateprocessor.postprocessors;

import org.nmcpye.datarun.datatemplateelement.AbstractElement;

/**
 * @author Hamza Assada, 18/03/2025
 */
public interface FormElementHandler<T extends AbstractElement> {
    FormElementHandler<T> linkWith(FormElementHandler<T> next);

    T process(T element);
}
