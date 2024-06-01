package org.nmcpye.datarun.service.custom.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.nmcpye.datarun.domain.*;
import org.nmcpye.datarun.service.custom.dto.AssignmentDTO;
import org.nmcpye.datarun.service.custom.dto.PatientInfoDTO;

/**
 * Mapper for the entity {@link PatientInfo} and its DTO {@link PatientInfoDTO}.
 */
@Mapper(componentModel = "spring")
public interface PatientInfoMapper extends EntityMapper<PatientInfoDTO, PatientInfo> {
    PatientInfoMapper INSTANCE = Mappers.getMapper(PatientInfoMapper.class);

    @Mapping(target = "location", source = "location", qualifiedByName = "assignmentCode")
    PatientInfoDTO toDto(PatientInfo s);

    @Named("assignmentCode")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "uid", source = "uid")
    @Mapping(target = "code", source = "code")
    AssignmentDTO toDtoAssignmentCode(Assignment assignment);
}
