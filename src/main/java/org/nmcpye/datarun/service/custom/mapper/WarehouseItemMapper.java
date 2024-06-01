package org.nmcpye.datarun.service.custom.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.nmcpye.datarun.domain.*;
import org.nmcpye.datarun.service.custom.dto.WarehouseItemDTO;

/**
 * Mapper for the entity {@link WarehouseItem} and its DTO {@link WarehouseItemDTO}.
 */
@Mapper(componentModel = "spring")
public interface WarehouseItemMapper extends EntityMapper<WarehouseItemDTO, WarehouseItem> {
    WarehouseItemMapper INSTANCE = Mappers.getMapper(WarehouseItemMapper.class);
}
