package org.nmcpye.datarun.datatemplateprocessor.postprocessors;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.dataelement.DataElement;

/**
 * @author Hamza Assada 18/03/2025 (7amza.it@gmail.com)
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
        ValueType actual   = source.getValueType();

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
