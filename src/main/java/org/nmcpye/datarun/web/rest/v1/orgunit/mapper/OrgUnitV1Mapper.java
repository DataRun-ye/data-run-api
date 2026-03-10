package org.nmcpye.datarun.web.rest.v1.orgunit.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.web.rest.v1.orgunit.dto.OrgUnitV1Dto;

import java.util.List;

/**
 * One-way mapper: OrgUnit entity → OrgUnitV1Dto (read-only facade).
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrgUnitV1Mapper {

    OrgUnitV1Dto toDto(OrgUnit entity);

    OrgUnitV1Dto.OrgUnitRefDto toRefDto(OrgUnit entity);

    List<OrgUnitV1Dto> toDtoList(List<OrgUnit> entities);
}
