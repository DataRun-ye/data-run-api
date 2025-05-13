//package org.nmcpye.datarun.mapper;
//
//import org.mapstruct.*;
//import org.nmcpye.datarun.drun.postgres.domain.AssignmentForm;
//import org.nmcpye.datarun.mapper.dto.AssignmentFormDto;
//import org.nmcpye.datarun.security.useraccess.dataform.FormAccessService;
//import org.springframework.beans.factory.annotation.Autowired;
//
//@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
//    componentModel = MappingConstants.ComponentModel.SPRING)
//public abstract class AssignmentFormMapper
//    implements BaseMapper<AssignmentFormDto, String> {
//
//    @Autowired
//    public FormAccessService formAccessService;
//
////    protected AssignmentFormMapper(AssignmentRepository assignmentRepository, FormAccessService formAccessService) {
////        this.assignmentRepository = assignmentRepository;
////        this.formAccessService = formAccessService;
////    }
//
//    @Mapping(target = ".", source = "dto.formUid")
//    public abstract String toEntity(AssignmentFormDto dto);
//
//    @Mappings({
////        @Mapping(target = "assignmentUid", source = "assignment.uid"),
//        @Mapping(target = "canViewSubmissions", expression = "java(formAccessService.canViewSubmissions(entity.getFormUid()))"),
//        @Mapping(target = "canEditSubmissions", expression = "java(formAccessService.canEditSubmissions(entity.getFormUid()))"),
//        @Mapping(target = "canAddSubmissions", expression = "java(formAccessService.canAddSubmissions(entity.getFormUid()))"),
//        @Mapping(target = "canApproveSubmissions", expression = "java(formAccessService.canApproveSubmissions(entity.getFormUid()))"),
//        @Mapping(target = "canDeleteSubmissions", expression = "java(formAccessService.canDeleteSubmissions(entity.getFormUid()))")
//    })
//    public abstract AssignmentFormDto toDto(AssignmentForm entity);
//}
