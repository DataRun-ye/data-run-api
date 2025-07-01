package org.nmcpye.datarun.schema;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

/**
 * @author Hamza Assada 01/05/2025 (7amza.it@gmail.com)
 */
public class ImportProcessor {
    private final Map<Class<?>, ImportableEntityHandler<?, ?>> handlers;

    public ImportProcessor(List<ImportableEntityHandler<?, ?>> handlerList) {
        this.handlers = handlerList.stream()
            .collect(toMap(ImportableEntityHandler::getEntityType, h -> h));
    }

    public <T, K> List<ImportIssue> dryRun(List<T> items, ImportContext context, Class<T> clazz) {
        ImportableEntityHandler<T, K> handler = resolveHandler(clazz);
        return handler.validate(items, context);
    }

    @Transactional
    public <T, K> void runImport(List<T> items, ImportContext ctx, Class<T> clazz
    ) {
        ImportableEntityHandler<T, K> h = resolveHandler(clazz);
        h.persist(items, ctx);
    }

    @SuppressWarnings("unchecked")
    private <T, K> ImportableEntityHandler<T, K> resolveHandler(Class<T> type) {
        return (ImportableEntityHandler<T, K>)
            Optional.ofNullable(handlers.get(type))
                .orElseThrow(() -> new IllegalArgumentException(
                    "No import handler for " + type.getSimpleName()));
    }
}
