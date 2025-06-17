//package org.nmcpye.datarun.security.useraccess.dataform;
//
//import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
//import org.nmcpye.datarun.common.feedback.ErrorCode;
//import org.nmcpye.datarun.userdetail.UserFormAccess;
//import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;
//import org.nmcpye.datarun.assignment.repository.AssignmentRepository;
//import org.nmcpye.datarun.team.repository.TeamRepository;
//import org.nmcpye.datarun.security.SecurityUtils;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Collection;
//import java.util.Set;
//
///**
// * @author Hamza Assada 24/04/2025 <7amza.it@gmail.com>
// */
//@Service
//@Transactional(readOnly = true)
//public class AssignmentFormAccessCachingService {
//    //    final static String USER_FLOW_INSTANCE_SUMMARIES_CACHE = "assignmentPages";
//    final public static String USER_FORM_ACCESS_CACHE = "formAccess";
//    private final AssignmentRepository assignmentRepository;
//    private final TeamRepository teamRepository;
//
//    public AssignmentFormAccessCachingService(AssignmentRepository assignmentRepository, TeamRepository teamRepository) {
//        this.assignmentRepository = assignmentRepository;
//        this.teamRepository = teamRepository;
//    }
//
//    //    @Cacheable(cacheNames = USER_FLOW_INSTANCE_SUMMARIES_CACHE, key = "#team + '_' + #pageable.pageNumber")
//    public Page<AssignmentSummary> fetchAssignmentPage(Collection<String> teams, Pageable pageable) {
//        return assignmentRepository.findSummariesTeam(teams, pageable);
//    }
//
//    @Cacheable(cacheNames = USER_FORM_ACCESS_CACHE, key = "#teamUID + '_' + #form")
//    public UserFormAccess getFormConfig(String teamUID, String form) {
//        final var user = SecurityUtils.getCurrentUserDetailsOrThrow();
//        final var team = teamRepository.findByUid(teamUID)
//            .orElseThrow(() -> new IllegalQueryException(ErrorCode.E3006, teamUID));
//
//        if (user.isSuper()) {
//            return UserFormAccess.builder()
//                .form(form)
//                .team(team.getUid())
//                .user(user.getUid())
//                .permissions(Set.of(FormPermission.values())).build();
//        }
//
//        return UserFormAccess.builder()
//            .form(form)
//            .team(team.getUid())
//            .user(user.getUid())
//            .permissions(team.getFormPermissions(form)).build();
//    }
//}
