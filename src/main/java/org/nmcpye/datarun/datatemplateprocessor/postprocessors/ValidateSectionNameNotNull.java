package org.nmcpye.datarun.datatemplateprocessor.postprocessors;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;

/**
 * @author Hamza Assada 18/03/2025 (7amza.it@gmail.com)
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
