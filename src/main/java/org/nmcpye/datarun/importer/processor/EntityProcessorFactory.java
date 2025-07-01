package org.nmcpye.datarun.importer.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * processors registry
 *
 * @author Hamza Assada 03/06/2025 (7amza.it@gmail.com)
 */
@Component
@Slf4j
public class EntityProcessorFactory {
    private final Set<ImportProcessor<?>> processors;

    private final Map<String, ImportProcessor<?>> processorsCache = new ConcurrentHashMap<>();

    public EntityProcessorFactory(Set<ImportProcessor<?>> processors) {
        this.processors = processors;
    }


    @SuppressWarnings("unchecked")
    public <T> ImportProcessor<T> getProcessor(String entityType) {
        return (ImportProcessor<T>) getObjectStore(entityType, processorsCache, processors);

    }

    private <S extends ImportProcessor<?>> S getObjectStore(
        String type,
        Map<String, S> cache,
        Set<S> stores
    ) {
        return cache.computeIfAbsent(
            type,
            key -> {
                S store = stores.stream().filter(s -> s.getEntityType().equals(key)).findFirst().orElse(null);
                if (store == null) {
                    // as this is within the "loader" function this will only get
                    // logged once
                    log.warn("No IdentifiableObjectStore found for class: '{}'", type);
                }
                return store;
            }
        );
    }
}
