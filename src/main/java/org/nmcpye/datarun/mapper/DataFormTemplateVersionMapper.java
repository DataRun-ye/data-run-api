package org.nmcpye.datarun.mapper;

import org.mapstruct.Mapper;
import org.nmcpye.datarun.mapper.dto.DataFormTemplateVersionDto;
import org.nmcpye.datarun.mongo.domain.DataFormTemplateVersion;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <10-05-2025>
 */
@Mapper(componentModel = "spring")
public interface DataFormTemplateVersionMapper
    extends BaseMapper<DataFormTemplateVersionDto, DataFormTemplateVersion> {
}
