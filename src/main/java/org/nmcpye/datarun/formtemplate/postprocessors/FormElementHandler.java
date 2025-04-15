package org.nmcpye.datarun.formtemplate.postprocessors;

import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;

/**
 * @author Hamza Assada, 18/03/2025
 */
public interface FormElementHandler<T extends FormElementConf> {
    FormElementHandler<T> linkWith(FormElementHandler<T> next);

    T process(T element);
}
