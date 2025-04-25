//package org.nmcpye.datarun.security.useraccess.dataform;
//
//import org.nmcpye.datarun.drun.postgres.domain.Team;
//
///**
// * @author Hamza Assada, 25/04/2025
// */
//public class FormAccessService {
//    boolean canViewSubmissions(String form, Team team) {
//        return team.getFormPermissions().stream()
//            .anyMatch(f -> f.canViewSubmission(form));
//    }
//
//    public boolean canViewSubmissionFromUsers(String form, Team team) {
//        return team.getFormPermissions().stream()
//            .anyMatch(f -> f.canViewSubmissionFromUsers(form));
//    }
//
//    public boolean canAddSubmission(String form, Team team) {
//        return team.getFormPermissions().stream()
//            .anyMatch(f -> f.canAddSubmission(form));
//    }
//
//    public boolean canEditSubmission(String form, Team team) {
//        return team.getFormPermissions().stream()
//            .anyMatch(f -> f.canEditSubmission(form));
//
//    }
//
//    public boolean canEditSubmissionFromUsers(String form, Team team) {
//        return team.getFormPermissions().stream()
//            .anyMatch(f -> f.canEditSubmissionFromUsers(form));
//    }
//
//    public boolean canApproveSubmission(String form, Team team) {
//        return team.getFormPermissions().stream()
//            .anyMatch(f -> f.canApproveSubmission(form));
//    }
//
//    public boolean canDeleteSubmission(String form, Team team) {
//        return team.getFormPermissions().stream()
//            .anyMatch(f -> f.canDeleteSubmission(form));
//    }
//
//    public boolean canDeleteSubmissionFromUsers(String form, Team team) {
//        return team.getFormPermissions().stream()
//            .anyMatch(f -> f.canDeleteSubmissionFromUsers(form));
//    }
//}
