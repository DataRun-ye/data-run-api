package org.nmcpye.datarun.web.rest.v1.optionset.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.nmcpye.datarun.jpa.option.Option;
import org.nmcpye.datarun.jpa.option.OptionSet;
import org.nmcpye.datarun.web.rest.v1.optionset.dto.OptionSetV1Dto;

import java.util.List;

/**
 * One-way mapper: OptionSet entity → OptionSetV1Dto (read-only facade).
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface OptionSetV1Mapper {

    OptionSetV1Dto toDto(OptionSet entity);

    OptionSetV1Dto.OptionV1Dto toOptionDto(Option entity);

    List<OptionSetV1Dto> toDtoList(List<OptionSet> entities);
}
