package org.nmcpye.datarun.importer.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.util.ProxyUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * a state of an import request and its progress
 *
 * @author Hamza Assada 04/06/2025 (7amza.it@gmail.com)
 */
@Accessors(fluent = true)
@Getter
@Setter
@ToString
public class ImportContext<E> {

    private final ImportRequest request;

    protected E entity;

    private final List<String> errors = new ArrayList<>();

    public ImportContext(ImportRequest request) {
        this.request = request;
    }

    /**
     * Get the actual type for the entity.
     *
     * @return entity's class type
     * @see ProxyUtils#getUserClass(Class)
     */
    @SuppressWarnings("unchecked")
    public Class<E> getEntityType() {
        return (Class<E>) ProxyUtils.getUserClass(entity().getClass());
    }
}
