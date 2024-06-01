package org.nmcpye.datarun.service.custom.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.nmcpye.datarun.domain.*;
import org.nmcpye.datarun.service.custom.dto.VillageLocationDTO;

/**
 * Mapper for the entity {@link VillageLocation} and its DTO {@link VillageLocationDTO}.
 */
@Mapper(componentModel = "spring")
public interface VillageLocationMapper extends EntityMapper<VillageLocationDTO, VillageLocation> {
    VillageLocationMapper INSTANCE = Mappers.getMapper(VillageLocationMapper.class);
}
