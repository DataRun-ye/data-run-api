package org.nmcpye.datarun.service.custom.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.nmcpye.datarun.domain.*;
import org.nmcpye.datarun.service.custom.dto.*;

/**
 * Mapper for the entity {@link WarehouseTransaction} and its DTO {@link WarehouseTransactionDTO}.
 */
@Mapper(componentModel = "spring")
public interface WarehouseTransactionMapper extends EntityMapper<WarehouseTransactionDTO, WarehouseTransaction> {
    WarehouseTransactionMapper INSTANCE = Mappers.getMapper(WarehouseTransactionMapper.class);

    @Mapping(target = "item", source = "item", qualifiedByName = "warehouseItemName")
    @Mapping(target = "sourceWarehouse", source = "sourceWarehouse", qualifiedByName = "warehouseName")
    @Mapping(target = "team", source = "team", qualifiedByName = "teamCode")
    @Mapping(target = "warehouse", source = "warehouse", qualifiedByName = "warehouseName")
    @Mapping(target = "activity", source = "activity", qualifiedByName = "activityCode")
    WarehouseTransactionDTO toDto(WarehouseTransaction s);

    @Named("warehouseItemName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "name", source = "name")
    WarehouseItemDTO toDtoWarehouseItemName(WarehouseItem warehouseItem);

    @Named("warehouseName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "name", source = "name")
    WarehouseDTO toDtoWarehouseName(Warehouse warehouse);

    @Named("teamCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "code", source = "code")
    TeamDTO toDtoTeamCode(Team team);

    @Named("activityCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "code", source = "code")
    ActivityDTO toDtoActivityCode(Activity activity);
}
