package org.nmcpye.datarun.datatemplateprocessor.postprocessors;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.datatemplateelement.FieldTemplateElementDto;
import org.nmcpye.datarun.jpa.dataelement.DataElement;

/**
 * @author Hamza Assada 18/03/2025 (7amza.it@gmail.com)
 */
public class ValidateReferenceTypeHandler
    extends AbstractTemplateElementHandler<FieldTemplateElementDto> {
    private final DataElement source;

    public ValidateReferenceTypeHandler(DataElement source) {
        this.source = source;
    }

    @Override
    protected FieldTemplateElementDto handle(FieldTemplateElementDto element) {
        if (source.getValueType().isReference() && element.getResourceMetadataSchema() == null) {
            throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1103, element.getParent(), element.getName()));
        }
        return element;
    }
}
