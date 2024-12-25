//package org.nmcpye.datarun.drun.postgres.service.impl;
//
//import org.nmcpye.datarun.drun.postgres.domain.Team;
//import org.nmcpye.datarun.drun.postgres.domain.TeamFormPermission;
//import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;
//import org.nmcpye.datarun.drun.postgres.repository.TeamFormPermissionRepository;
//import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Set;
//
//@Service
//@Transactional
//public class TeamFormPermissionService {
//    private final TeamFormPermissionRepository repository;
//    private final TeamRelationalRepositoryCustom teamRepository;
//
//    // Set<FormPermission> permissions = EnumSet.of(FormPermission.READ, FormPermission.WRITE)
//
//    public TeamFormPermissionService(TeamFormPermissionRepository repository, TeamRelationalRepositoryCustom teamRepository) {
//        this.repository = repository;
//        this.teamRepository = teamRepository;
//    }
//
//    public void assignPermissions(Long teamId, String formTemplateId, Set<FormPermission> permissions) {
//        Team team = teamRepository.findById(teamId)
//            .orElseThrow(() -> new IllegalArgumentException("Invalid team ID"));
//        TeamFormPermission permission = new TeamFormPermission(team, formTemplateId, permissions);
//        repository.save(permission);
//    }
//
//    public boolean hasPermission(Long teamId, String formTemplateId, FormPermission permission) {
//        return repository.findByTeamIdAndForm(teamId, formTemplateId)
//            .map(perm -> perm.getPermissions().contains(permission))
//            .orElse(false);
//    }
//}
