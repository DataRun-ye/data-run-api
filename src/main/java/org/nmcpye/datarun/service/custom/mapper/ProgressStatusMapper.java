package org.nmcpye.datarun.service.custom.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.nmcpye.datarun.domain.*;
import org.nmcpye.datarun.service.custom.dto.ProgressStatusDTO;

/**
 * Mapper for the entity {@link ProgressStatus} and its DTO {@link ProgressStatusDTO}.
 */
@Mapper(componentModel = "spring")
public interface ProgressStatusMapper extends EntityMapper<ProgressStatusDTO, ProgressStatus> {
    ProgressStatusMapper INSTANCE = Mappers.getMapper(ProgressStatusMapper.class);
}
