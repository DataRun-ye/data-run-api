package org.nmcpye.datarun.formtemplate.postprocessors;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;

/**
 * @author Hamza, 18/03/2025
 */
public class ValidateReferenceTypeHandler
    extends AbstractFormElementHandler<FormDataElementConf> {
    private final DataElement source;

    public ValidateReferenceTypeHandler(DataElement source) {
        this.source = source;
    }

    @Override
    protected FormDataElementConf handle(FormDataElementConf element) {
        if (source.getType().isReference() && element.getResourceMetadataSchema() == null) {
            throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1103, element.getParent(), element.getName()));
        }
        return element;
    }
}
