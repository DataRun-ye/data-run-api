package org.nmcpye.datarun.importer.processor;

import org.nmcpye.datarun.importer.config.ImportContext;
import org.nmcpye.datarun.importer.config.ImportRequest;
import org.nmcpye.datarun.importer.config.ImportResult;
import org.nmcpye.datarun.importer.identifier.IdentifierStrategy;

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
public abstract class AbstractEntityProcessor<E>
    implements ImportProcessor<E> {

    private final Map<String, IdentifierStrategy> strategies;

    protected AbstractEntityProcessor(Map<String, IdentifierStrategy> strategies) {
        this.strategies = strategies;
    }


    // We have to make sure the `ImportProcessor` is a Spring bean? Actually, the abstract class is not a bean. The concrete classes are.
    //
    // We can have a service that routes the import request to the appropriate processor based on the entity type.
    //
    // Example:
    // public interface ImportService {
    //ImportResult importEntity(ImportRequest request, String entityType);
    //
    //}
    public ImportResult<E> process(ImportRequest request) {
        ImportContext<E> context = new ImportContext<>(request);

        // Step 1: Parse
        context.entity(parse(context));

        // Step 2: Resolve associations
        // the `resolveAssociations` might also require validation (like checking
        // existence of referenced entities). So we might combine step2 and step3?
        // Or do the association resolution and then validate the resolved entity.
        resolveAssociations(context);

        // Step 3: Validate
        validate(context);

        if (!context.errors().isEmpty())
            return ImportResult.failure(context.errors());

        // If dry-run, return success without persisting
        if (request.dryRun()) return ImportResult.success(context.entity(), true);

        // Step 4: Persist
        persist(context);
        return ImportResult.success(context.entity(), false);
    }

    protected abstract E parse(ImportContext<E> context);

    protected abstract void resolveAssociations(ImportContext<E> context);

    protected abstract void validate(ImportContext<E> context);

    protected abstract void persist(ImportContext<E> context);
}
