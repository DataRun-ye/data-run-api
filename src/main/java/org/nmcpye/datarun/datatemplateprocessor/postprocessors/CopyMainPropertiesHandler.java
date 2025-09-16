package org.nmcpye.datarun.datatemplateprocessor.postprocessors;

import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.jpa.dataelement.DataElement;

import java.util.Optional;

/**
 * @author Hamza Assada 18/03/2025 (7amza.it@gmail.com)
 */
public class CopyMainPropertiesHandler
    extends AbstractFormElementHandler<FormDataElementConf> {
    private final DataElement source;

    public CopyMainPropertiesHandler(DataElement source) {
        this.source = source;
    }

    @Override
    protected FormDataElementConf handle(FormDataElementConf element) {
        element.setId(source.getUid());
        element.setCode(source.getCode());
        element.setType(source.getValueType());
        element.setName(source.getName());
        element.setIsDimension(source.getIsDimension());
        element.setIsMeasure(source.getIsMeasure());
        element.setAggregationType(source.getAggregationType());
        element.setDescription(Optional.ofNullable(element.getDescription()).orElse(source.getDescription()));
        element.setLabel(Optional.ofNullable(element.getLabel()).orElse(source.getLabel()));
        return element;
    }
}
