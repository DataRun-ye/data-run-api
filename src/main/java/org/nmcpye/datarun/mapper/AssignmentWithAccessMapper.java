//package org.nmcpye.datarun.mapper;
//
//import org.mapstruct.Mapper;
//import org.mapstruct.MappingConstants;
//import org.mapstruct.ReportingPolicy;
//import org.nmcpye.datarun.drun.postgres.domain.Assignment;
//import org.nmcpye.datarun.mapper.dto.AssignmentWithAccessDto;
//import org.nmcpye.datarun.security.useraccess.dataform.FormAccessService;
//
//@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
//    componentModel = MappingConstants.ComponentModel.SPRING)
//public abstract class AssignmentWithAccessMapper
//    implements BaseMapper<AssignmentWithAccessDto, Assignment> {
//    protected final FormAccessService formAccessService;
//
//    protected AssignmentWithAccessMapper(FormAccessService formAccessService) {
//        this.formAccessService = formAccessService;
//    }
//
//    @Override
//    public Assignment toEntity(AssignmentWithAccessDto dto) {
//        return null;
//    }
//
//    @Override
//    public AssignmentWithAccessDto toDto(Assignment entity) {
//        return null;
//    }
//}
