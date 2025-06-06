package org.nmcpye.datarun.importer.processor;

import org.nmcpye.datarun.importer.config.ImportContext;
import org.nmcpye.datarun.importer.identifier.IdentifierStrategy;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.common.IdentifiableObjectManager;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Now, for expandability:
 * <pre>
 * - We can later add pre-processing and post-processing steps by breaking
 * the process method into more steps.
 * - We can also allow conditional steps by having a list of steps in the
 * processor and iterating over them.
 * </pre>
 *
 * @author Hamza Assada 03/06/2025 <7amza.it@gmail.com>
 */
@Component
public class ActivityProcessor
    extends AbstractEntityProcessor<Activity> {
    private final IdentifiableObjectManager identifiableObjectManager;

    protected ActivityProcessor(Map<String, IdentifierStrategy> strategies, IdentifiableObjectManager identifiableObjectManager) {
        super(strategies);
        this.identifiableObjectManager = identifiableObjectManager;
    }

    @Override
    protected Activity parse(ImportContext<Activity> context) {
        return null;
    }

    @Override
    protected void resolveAssociations(ImportContext<Activity> context) {

    }

    @Override
    protected void validate(ImportContext<Activity> context) {

    }

    @Override
    protected void persist(ImportContext<Activity> context) {

    }

    @Override
    public String getEntityType() {
        return "activity";
    }
}
