package org.nmcpye.datarun.formtemplate.postprocessors;

import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.ValueTypeRendering;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.enumeration.RuleAction;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;

import java.util.Optional;

/**
 * @author Hamza, 18/03/2025
 */
public class MigrateDeprecatedPropertiesHandler
    extends AbstractFormElementHandler<FormDataElementConf> {
    private final DataElement source;

    public MigrateDeprecatedPropertiesHandler(DataElement source) {
        this.source = source;
    }

    @Override
    protected FormDataElementConf handle(FormDataElementConf element) {
        final var errorRule = element.getRules().stream()
            .filter(r -> r.getAction() == RuleAction.Error)
            .findAny();
        if (element.getConstraint() == null && errorRule.isPresent()) {
            element.setConstraint(errorRule.get().getExpression());
            element.setConstraintMessage(errorRule.get().getMessage());
        }

        if (source.getType() == ValueType.ScannedCode) {
            element
                .setValueTypeRendering(Optional.ofNullable(element.getValueTypeRendering())
                    .orElse(ValueTypeRendering.BAR_CODE));

            if (element.getGs1Enabled() == Boolean.TRUE) {
                element.setValueTypeRendering(Optional.ofNullable(element.getValueTypeRendering())
                    .orElse(ValueTypeRendering.GS1_DATAMATRIX));
            }
        }

        return element;
    }
}
