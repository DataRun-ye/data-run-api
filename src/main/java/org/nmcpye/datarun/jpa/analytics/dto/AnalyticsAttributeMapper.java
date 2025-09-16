package org.nmcpye.datarun.jpa.analytics.dto;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.nmcpye.datarun.jpa.analytics.domain.AnalyticsAttribute;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.SPRING)
public interface AnalyticsAttributeMapper {
    AnalyticsAttribute toEntity(AnalyticsAttributeDto dto);

    AnalyticsAttributeDto toDto(AnalyticsAttribute analyticsAttribute);

    List<AnalyticsAttributeDto> toDtoList(List<AnalyticsAttribute> analyticsAttributes);
}
