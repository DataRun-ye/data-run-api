//package org.nmcpye.datarun.mongo.service.impl;
//
//import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;
//import org.nmcpye.datarun.mongo.domain.TeamFormPermission;
//import org.nmcpye.datarun.mongo.repository.TeamFormPermissionRepository;
//import org.springframework.stereotype.Service;
//
//import java.util.Set;
//
//@Service
//public class TeamFormPermissionService {
//    private final TeamFormPermissionRepository repository;
//
//    public TeamFormPermissionService(TeamFormPermissionRepository repository) {
//        this.repository = repository;
//    }
//
//    public void assignPermissions(String teamId, String formTemplateId, Set<FormPermission> permissions) {
//        TeamFormPermission permission = new TeamFormPermission(teamId, formTemplateId, permissions);
//        repository.save(permission);
//    }
//
//    public boolean hasPermission(String teamId, String formTemplateId, FormPermission permission) {
//        return repository.findByTeamAndForm(teamId, formTemplateId)
//            .map(perm -> perm.getPermissions().contains(permission))
//            .orElse(false);
//    }
//}
