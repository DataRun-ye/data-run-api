package org.nmcpye.datarun.formtemplateprocessor.postprocessors;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;

/**
 * @author Hamza Assada, 18/03/2025
 */
public class ValidateValueTypeHandler
    extends AbstractFormElementHandler<FormDataElementConf> {

    private final DataElement source;

    public ValidateValueTypeHandler(DataElement source) {
        this.source = source;
    }

    @Override
    protected FormDataElementConf handle(FormDataElementConf element) {
        ValueType declared = element.getType();
        ValueType actual   = source.getType();

        if (declared != null && !declared.isCompatible(actual)) {
            // E1105: element ID, declared type, actual type
            throw new IllegalQueryException(
                new ErrorMessage(
                    ErrorCode.E1105,
                    element.getId(),
                    declared,
                    actual
                )
            );
        }

        // ensure the element ends up with the source type
        return element.type(actual);
    }
}
