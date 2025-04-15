package org.nmcpye.datarun.formtemplate.postprocessors;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;

/**
 * @author Hamza Assada, 18/03/2025
 */
public class ValidateSectionNameNotNull
    extends AbstractFormElementHandler<FormSectionConf> {

    @Override
    protected FormSectionConf handle(FormSectionConf element) {
        if (element.getName() == null) {
            throw new IllegalQueryException("Section name must not be null");
        }
        return element;
    }
}
