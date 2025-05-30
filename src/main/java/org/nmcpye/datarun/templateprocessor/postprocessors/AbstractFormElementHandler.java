package org.nmcpye.datarun.templateprocessor.postprocessors;

import org.nmcpye.datarun.dataelement.DataElement;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.springframework.lang.Nullable;

/**
 * @author Hamza Assada, 18/03/2025
 */
public abstract class AbstractFormElementHandler<T extends FormElementConf>
    implements FormElementHandler<T> {

    private FormElementHandler<T> next;

    public static FormDataElementConf processElement(FormDataElementConf element,
                                                     FormWithFields formTemplate, @Nullable DataElement source) {

        FormElementHandler<FormDataElementConf> handlerChain = new ValidateValueTypeHandler(source);
        handlerChain
            .linkWith(new CopyMainPropertiesHandler(source))
            .linkWith(new ValidateReferenceTypeHandler(source))
            .linkWith(new MigrateDeprecatedPropertiesHandler(source));

        return handlerChain.process(element);
    }

    public static FormSectionConf processSection(FormSectionConf element) {

        FormElementHandler<FormSectionConf> handlerChain = new ValidateSectionNameNotNull();
        return handlerChain.process(element);
    }

    @Override
    public FormElementHandler<T> linkWith(FormElementHandler<T> next) {
        this.next = next;
        return next;
    }

    @Override
    public T process(T element) {
        T processedElement = handle(element);
        if (next != null) {
            return next.process(processedElement);
        }
        return processedElement;
    }

    protected abstract T handle(T element);
}
