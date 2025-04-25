//package org.nmcpye.datarun.security.useraccess.dataform;
//
//import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
//import org.nmcpye.datarun.common.feedback.ErrorCode;
//import org.nmcpye.datarun.common.repository.UserRepository;
//import org.nmcpye.datarun.common.security.UserFormAccess;
//import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;
//import org.nmcpye.datarun.drun.postgres.repository.AssignmentRepository;
//import org.nmcpye.datarun.drun.postgres.repository.TeamRepository;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Collection;
//import java.util.List;
//import java.util.Set;
//
///**
// * @author Hamza Assada, 24/04/2025
// */
//@Service
//@Transactional(readOnly = true)
//public class AssignmentFormAccessCachingService {
//    //    final static String USER_ASSIGNMENT_SUMMARIES_CACHE = "assignmentPages";
////    final public static String USER_FORM_ACCESS_CACHE = "formAccess";
//    private final AssignmentRepository assignmentRepository;
//    private final TeamRepository teamRepository;
//    final private UserRepository userRepository;
//
//    public AssignmentFormAccessCachingService(AssignmentRepository assignmentRepository, TeamRepository teamRepository, UserRepository userRepository) {
//        this.assignmentRepository = assignmentRepository;
//        this.teamRepository = teamRepository;
//        this.userRepository = userRepository;
//    }
//
//    //    @Cacheable(cacheNames = USER_ASSIGNMENT_SUMMARIES_CACHE, key = "#team + '_' + #pageable.pageNumber")
//    public Page<AssignmentSummary> fetchAssignmentPage(Collection<String> teams, Pageable pageable) {
//        return assignmentRepository.findSummariesTeam(teams, pageable);
//    }
//
////    @Cacheable(cacheNames = USER_FORM_ACCESS_CACHE, key = "#teamUID + '_' + #userLogin")
//    public List<String> getFormConfig(String teamUID, String userLogin) {
//        final var user = userRepository.findOneWithAuthoritiesByLogin(userLogin).orElseThrow(() ->
//            new UsernameNotFoundException("User with login " + userLogin + " was not found in the database"));
//
//        final var team = teamRepository.findByUid(teamUID)
//            .orElseThrow(() -> new IllegalQueryException(ErrorCode.E3006, teamUID));
//
//        if (user.isSuper()) {
//            return UserFormAccess.builder()
//                .form(form)
//                .team(team.getUid())
////                .user(user.getUid())
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
