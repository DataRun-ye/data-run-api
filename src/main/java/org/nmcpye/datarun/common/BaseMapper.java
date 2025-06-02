package org.nmcpye.datarun.common;

import java.util.List;

/**
 * Contract for a generic dto to entity mapper.
 *
 * @param <D> - DTO type parameter.
 * @param <E> - Entity type parameter.
 */

public interface BaseMapper<D, E> {
    E toEntity(D dto);

    D toDto(E entity);

//    List<E> toEntities(List<D> dtoList);

    List<D> toDtoList(List<E> entityList);
}
