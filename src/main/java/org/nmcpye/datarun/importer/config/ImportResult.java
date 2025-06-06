package org.nmcpye.datarun.importer.config;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author Hamza Assada 03/06/2025 <7amza.it@gmail.com>
 */
@Builder
@Value
public class ImportResult<T> {
    boolean success;
    T entity;
    List<String> errors;
    boolean dryRun;

    public static <E> ImportResult<E> failure(List<String> errors) {
        return ImportResult.<E>builder()
            .success(false).errors(errors).build();
    }

    public static <E> ImportResult<E> success(E entity, boolean dryRun) {
        return ImportResult.<E>builder()
            .success(true).entity(entity).dryRun(dryRun).build();
    }
}
