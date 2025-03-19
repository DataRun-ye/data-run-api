package org.nmcpye.datarun.formtemplate.postprocessors;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;

/**
 * @author Hamza, 18/03/2025
 */
public class ValidateValueTypeHandler
    extends AbstractFormElementHandler<FormDataElementConf> {
    private final DataElement source;

    public ValidateValueTypeHandler(DataElement source) {
        this.source = source;
    }

    @Override
    protected FormDataElementConf handle(FormDataElementConf element) {
        if (element.getType() != null && element.getType() != source.getType()) {
            throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1105, element.getId(), element.getType(), source.getType()));
        }

        return element.type(source.getType());
    }
}

