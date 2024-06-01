package org.nmcpye.datarun.service.custom.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.nmcpye.datarun.domain.*;
import org.nmcpye.datarun.service.custom.dto.ItnsVillageDTO;
import org.nmcpye.datarun.service.custom.dto.ItnsVillageHousesDetailDTO;

/**
 * Mapper for the entity {@link ItnsVillageHousesDetail} and its DTO {@link ItnsVillageHousesDetailDTO}.
 */
@Mapper(componentModel = "spring")
public interface ItnsVillageHousesDetailMapper extends EntityMapper<ItnsVillageHousesDetailDTO, ItnsVillageHousesDetail> {
    ItnsVillageHousesDetailMapper INSTANCE = Mappers.getMapper(ItnsVillageHousesDetailMapper.class);

    @Mapping(target = "villageData", source = "villageData", qualifiedByName = "itnsVillageId")
    ItnsVillageHousesDetailDTO toDto(ItnsVillageHousesDetail s);

    @Named("itnsVillageId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    ItnsVillageDTO toDtoItnsVillageId(ItnsVillage itnsVillage);
}
