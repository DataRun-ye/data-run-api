package org.nmcpye.datarun.formtemplate.postprocessors;

import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;

import java.util.Optional;

/**
 * @author Hamza Assada, 18/03/2025
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
        element.setType(source.getType());
        element.setName(Optional.ofNullable(element.getName()).orElse(source.getName()));
        element.setDescription(Optional.ofNullable(element.getDescription()).orElse(source.getDescription()));
        element.setLabel(Optional.ofNullable(element.getLabel()).orElse(source.getLabel()));
        return element;
    }
}
