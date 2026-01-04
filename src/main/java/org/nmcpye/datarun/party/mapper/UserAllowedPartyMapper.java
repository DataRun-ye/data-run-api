package org.nmcpye.datarun.party.mapper;

import org.mapstruct.*;
import org.mapstruct.MappingConstants.ComponentModel;
import org.nmcpye.datarun.party.dto.UserAllowedPartyDto;
import org.nmcpye.datarun.party.entities.UserAllowedParty;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = ComponentModel.SPRING)
public interface UserAllowedPartyMapper {
    @Mapping(source = "idPartyId", target = "id.partyId")
    @Mapping(source = "idUserId", target = "id.userId")
    UserAllowedParty toEntity(UserAllowedPartyDto userAllowedPartyDto);

    @InheritInverseConfiguration(name = "toEntity")
    UserAllowedPartyDto toDto(UserAllowedParty userAllowedParty);

    @InheritConfiguration(name = "toEntity")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserAllowedParty partialUpdate(@MappingTarget UserAllowedParty userAllowedParty, UserAllowedPartyDto userAllowedPartyDto);
}
