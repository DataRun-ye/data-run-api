package org.nmcpye.datarun.datatemplateprocessor.postprocessors;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.datatemplateelement.SectionTemplateElementDto;

/**
 * @author Hamza Assada 18/03/2025 (7amza.it@gmail.com)
 */
public class ValidateSectionNameNotNull
    extends AbstractTemplateElementHandler<SectionTemplateElementDto> {

    @Override
    protected SectionTemplateElementDto handle(SectionTemplateElementDto element) {
        if (element.getName() == null) {
            throw new IllegalQueryException("Section name must not be null");
        }

        return element;
    }
}
