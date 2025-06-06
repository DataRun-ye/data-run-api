//package org.nmcpye.datarun.security.useraccess.dataform;
//
//import org.nmcpye.datarun.userdetail.UserFormAccess;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Collection;
//import java.util.List;
//
///**
// * @author Hamza Assada 24/04/2025 <7amza.it@gmail.com>
// */
//@Service
//@Transactional(readOnly = true)
//public class AssignmentFormAccessResolver {
//    private final AssignmentFormAccessCachingService formAccessCachingService;
//
//    public AssignmentFormAccessResolver(AssignmentFormAccessCachingService formAccessCachingService) {
//        this.formAccessCachingService = formAccessCachingService;
//    }
//
//    public Page<AssignmentFormAccessDto> resolveForTeam(Collection<String> teams, Pageable pageable) {
//        return formAccessCachingService.fetchAssignmentPage(teams, pageable)
//            .map(this::toDto);
//    }
//
//    public AssignmentFormAccessDto toDto(AssignmentSummary assignmentSummary) {
//        List<UserFormAccess> configs = assignmentSummary.getForms().stream()
//            .map(form -> formAccessCachingService
//                .getFormConfig(assignmentSummary.getTeam().getUid(), form))
//            .toList();
//        return AssignmentFormAccessDto.builder()
//            .assignment(assignmentSummary.getUid())
//            .team(assignmentSummary.getTeam().getUid())
//            .orgUnit(assignmentSummary.getOrgUnit().getUid())
//            .forms(configs)
//            .build();
//    }
//}
