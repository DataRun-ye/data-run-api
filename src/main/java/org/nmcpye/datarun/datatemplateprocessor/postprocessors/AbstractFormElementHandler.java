package org.nmcpye.datarun.datatemplateprocessor.postprocessors;

import org.nmcpye.datarun.datatemplateelement.AbstractElement;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.dataelement.DataTemplateElement;
import org.springframework.lang.Nullable;

/**
 * @author Hamza Assada 18/03/2025 <7amza.it@gmail.com>
 */
public abstract class AbstractFormElementHandler<T extends AbstractElement>
    implements FormElementHandler<T> {

    private FormElementHandler<T> next;

    public static FormDataElementConf processElement(FormDataElementConf element, @Nullable DataTemplateElement source) {

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
